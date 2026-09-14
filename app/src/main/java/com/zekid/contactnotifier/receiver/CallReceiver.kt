package com.zekid.contactnotifier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.zekid.contactnotifier.data.ContactRepository
import com.zekid.contactnotifier.data.NtfyRepository
import com.zekid.contactnotifier.data.SettingsRepository
import com.zekid.contactnotifier.service.NotificationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("CallReceiver", "[DEBUG-CALL] Received broadcast: ${intent.action}")
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("CallReceiver", "[DEBUG-CALL] State: $state")
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("CallReceiver", "[DEBUG-CALL] Phone Number: ${phoneNumber ?: "NULL (Check READ_CALL_LOG permission)"}")
                if (phoneNumber != null) {
                    val pendingResult = goAsync()
                    scope.launch {
                        try {
                            val settingsRepository = SettingsRepository(context)
                            val settings = settingsRepository.appSettingsFlow.first()
                            Log.d("CallReceiver", "[DEBUG-CALL] Settings: enabled=${settings.callNotificationsEnabled}, topic=${settings.ntfyTopic}")
                            
                            if (settings.callNotificationsEnabled) {
                                val contactRepository = ContactRepository(context)
                                val ntfyRepository = NtfyRepository(settingsRepository)
                                val dispatcher = NotificationDispatcher(contactRepository, ntfyRepository)
                                
                                val success = dispatcher.dispatchCallNotification(phoneNumber)
                                Log.d("CallReceiver", "[DEBUG-CALL] Dispatch success: $success")
                            } else {
                                Log.d("CallReceiver", "[DEBUG-CALL] Call notifications disabled in settings")
                            }
                        } catch (e: Exception) {
                            Log.e("CallReceiver", "[DEBUG-CALL] Error processing call notification", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
