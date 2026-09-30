package com.zekid.contactnotifier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.zekid.contactnotifier.data.ContactRepository
import com.zekid.contactnotifier.data.NtfyRepository
import com.zekid.contactnotifier.data.SettingsRepository
import com.zekid.contactnotifier.service.DispatchDeduper
import com.zekid.contactnotifier.service.NotificationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "[DEBUG-SMS] Received broadcast: ${intent.action}")
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.w("SmsReceiver", "[DEBUG-SMS] Unexpected action: ${intent.action}")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        Log.d("SmsReceiver", "[DEBUG-SMS] Number of messages: ${messages.size}")
        
        if (messages.isEmpty()) {
            Log.w("SmsReceiver", "[DEBUG-SMS] No messages found in intent")
            return
        }

        val smsMessage = messages[0]
        val phoneNumber = smsMessage.displayOriginatingAddress
        val messageBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val snippet = DispatchDeduper.snippetOf(messageBody)
        
        Log.d("SmsReceiver", "[DEBUG-SMS] Sender: ${phoneNumber ?: "NULL"}, Body length: ${messageBody.length}")

        if (phoneNumber == null) {
            Log.e("SmsReceiver", "[DEBUG-SMS] Phone number is null, cannot dispatch")
            return
        }

        if (!DispatchDeduper.tryMark(phoneNumber, snippet)) {
            Log.d("SmsReceiver", "[DEBUG-SMS] Duplicate of recently dispatched message, skipping")
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                val settingsRepository = SettingsRepository(context)
                val settings = settingsRepository.appSettingsFlow.first()
                
                Log.d("SmsReceiver", "[DEBUG-SMS] Config: url=${settings.ntfyServerUrl}, topic=${settings.ntfyTopic}, enabled=${settings.smsNotificationsEnabled}")
                
                if (settings.ntfyTopic.isBlank()) {
                    Log.e("SmsReceiver", "[DEBUG-SMS] NTFY Topic is BLANK. Please set it in Settings and click SAVE.")
                } else if (!settings.smsNotificationsEnabled) {
                    Log.w("SmsReceiver", "[DEBUG-SMS] SMS notifications are DISABLED in settings.")
                } else {
                    val contactRepository = ContactRepository(context)
                    val ntfyRepository = NtfyRepository(settingsRepository)
                    val dispatcher = NotificationDispatcher(contactRepository, ntfyRepository, settingsRepository)
                    
                    val success = dispatcher.dispatchSmsNotification(phoneNumber, snippet)
                    if (success) {
                        Log.i("SmsReceiver", "[DEBUG-SMS] Notification dispatched successfully for $phoneNumber")
                    } else {
                        Log.e("SmsReceiver", "[DEBUG-SMS] Failed to dispatch notification for $phoneNumber")
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "[DEBUG-SMS] Error in background processing", e)
            } finally {
                Log.d("SmsReceiver", "[DEBUG-SMS] Finishing async broadcast")
                pendingResult.finish()
            }
        }
    }
}
