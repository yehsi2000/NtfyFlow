package com.zekid.contactnotifier.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.zekid.contactnotifier.data.ContactRepository
import com.zekid.contactnotifier.data.NtfyRepository
import com.zekid.contactnotifier.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fallback message path for transports that never hit the telephony stack —
 * Samsung Chat+ (RCS) and Google Messages RCS. Reads posted message
 * notifications instead of SMS broadcasts, then reuses the same
 * [NotificationDispatcher] pipeline (contact lookup + ntfy send).
 *
 * Requires the user to grant notification access in system settings and to
 * enable the in-app "notification listener" toggle. Muted conversations that
 * post no notification are invisible to this listener.
 */
class MessageNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        Log.i(TAG, "[DEBUG-NLS] Listener connected")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in MESSAGE_PACKAGES) return
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "[DEBUG-NLS] Skipping group summary from ${sbn.packageName}")
            return
        }

        val extras = notification.extras ?: return
        val messages = try {
            MessageNotificationParser.extractMessages(extras)
        } catch (e: RuntimeException) {
            Log.e(TAG, "[DEBUG-NLS] Cannot parse message notification from ${sbn.packageName}", e)
            return
        }
        Log.d(TAG, "[DEBUG-NLS] package=${sbn.packageName}, messages=${messages.size}, keys=${extras.keySet().sorted()}")
        if (messages.isEmpty()) return

        scope.launch {
            try {
                val appContext = applicationContext
                val settingsRepository = SettingsRepository(appContext)
                val settings = settingsRepository.appSettingsFlow.first()

                if (!settings.notificationListenerEnabled) {
                    Log.w(TAG, "[DEBUG-NLS] Chat+ / RCS forwarding is disabled in settings")
                    return@launch
                }
                if (settings.ntfyTopic.isBlank()) {
                    Log.e(TAG, "[DEBUG-NLS] NTFY topic blank, skipping")
                    return@launch
                }

                val contactRepository = ContactRepository(appContext)
                val dispatcher = NotificationDispatcher(
                    contactRepository,
                    NtfyRepository(settingsRepository),
                    settingsRepository
                )
                for ((sender, body) in messages) {
                    val senderNumber = resolveToNumber(contactRepository, sender)
                    if (!DispatchDeduper.tryMark(senderNumber, body)) continue
                    val success = dispatcher.dispatchSmsNotification(senderNumber, body)
                    Log.d(TAG, "[DEBUG-NLS] Dispatch success=$success, bodyLength=${body.length}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[DEBUG-NLS] Error processing notification", e)
            }
        }
    }

    private suspend fun resolveToNumber(
        contactRepository: ContactRepository,
        sender: String
    ): String {
        if (sender.any { it.isDigit() } && sender.all { it.isDigit() || it in "+-() ./ " }) {
            return sender
        }
        return try {
            contactRepository.findPhoneNumberByName(sender) ?: sender
        } catch (e: SecurityException) {
            Log.w(TAG, "[DEBUG-NLS] READ_CONTACTS not granted, using raw sender")
            sender
        }
    }

    companion object {
        private const val TAG = "MsgNotifListener"
        private val MESSAGE_PACKAGES = setOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging"
        )
    }
}
