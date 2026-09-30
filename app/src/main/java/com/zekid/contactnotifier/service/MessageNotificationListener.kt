package com.zekid.contactnotifier.service

import android.app.Notification
import android.os.Bundle
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
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: return
        val (sender, body) = extractMessage(extras) ?: return
        if (sender.isBlank() || body.isBlank()) return

        scope.launch {
            try {
                val appContext = applicationContext
                val settingsRepository = SettingsRepository(appContext)
                val settings = settingsRepository.appSettingsFlow.first()

                if (!settings.notificationListenerEnabled) return@launch
                if (settings.ntfyTopic.isBlank()) {
                    Log.e(TAG, "[DEBUG-NLS] NTFY topic blank, skipping")
                    return@launch
                }

                val contactRepository = ContactRepository(appContext)
                val senderNumber = resolveToNumber(contactRepository, sender)
                val snippet = DispatchDeduper.snippetOf(body)

                if (!DispatchDeduper.tryMark(senderNumber, snippet)) {
                    Log.d(TAG, "[DEBUG-NLS] Duplicate of recently dispatched message, skipping")
                    return@launch
                }

                Log.d(TAG, "[DEBUG-NLS] Dispatching message from $senderNumber")
                val dispatcher = NotificationDispatcher(
                    contactRepository,
                    NtfyRepository(settingsRepository),
                    settingsRepository
                )
                dispatcher.dispatchSmsNotification(senderNumber, snippet)
            } catch (e: Exception) {
                Log.e(TAG, "[DEBUG-NLS] Error processing notification", e)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extractMessage(extras: Bundle): Pair<String, String>? {
        // MessagingStyle first: RCS/chat apps usually post this shape.
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { it as? Notification.MessagingStyle.Message }
            ?.filter { !it.text.isNullOrBlank() }
        val last = messages?.lastOrNull()
        if (last != null) {
            val sender = last.sender?.toString()?.ifBlank { null }
                ?: conversationTitle(extras)
                ?: return null
            return sender to last.text.toString()
        }

        val sender = conversationTitle(extras) ?: return null
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.ifBlank { null }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.ifBlank { null }
            ?: return null
        return sender to body
    }

    private fun conversationTitle(extras: Bundle): String? =
        extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.ifBlank { null }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.ifBlank { null }

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
