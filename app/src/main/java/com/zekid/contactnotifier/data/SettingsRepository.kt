package com.zekid.contactnotifier.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * ntfy importance (1=min .. 5=urgent). Urgent (5) rings even in Do-Not-Disturb
 * on the receiving phone's ntfy app — use it for calls / known contacts.
 */
data class AppSettings(
    val ntfyServerUrl: String,
    val ntfyTopic: String,
    val callNotificationsEnabled: Boolean,
    val smsNotificationsEnabled: Boolean,
    val notificationListenerEnabled: Boolean,
    val callPriority: Int,
    val smsPriority: Int,
    val contactCallPriority: Int,
    val contactSmsPriority: Int
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val NTFY_SERVER_URL = stringPreferencesKey("ntfy_server_url")
        val NTFY_TOPIC = stringPreferencesKey("ntfy_topic")
        val CALL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("call_notifications_enabled")
        val SMS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("sms_notifications_enabled")
        val NOTIFICATION_LISTENER_ENABLED = booleanPreferencesKey("notification_listener_enabled")
        val CALL_PRIORITY = intPreferencesKey("call_priority")
        val SMS_PRIORITY = intPreferencesKey("sms_priority")
        val CONTACT_CALL_PRIORITY = intPreferencesKey("contact_call_priority")
        val CONTACT_SMS_PRIORITY = intPreferencesKey("contact_sms_priority")
    }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                ntfyServerUrl = preferences[PreferencesKeys.NTFY_SERVER_URL] ?: "https://ntfy.sh",
                ntfyTopic = preferences[PreferencesKeys.NTFY_TOPIC] ?: "",
                callNotificationsEnabled = preferences[PreferencesKeys.CALL_NOTIFICATIONS_ENABLED] ?: true,
                smsNotificationsEnabled = preferences[PreferencesKeys.SMS_NOTIFICATIONS_ENABLED] ?: true,
                notificationListenerEnabled = preferences[PreferencesKeys.NOTIFICATION_LISTENER_ENABLED] ?: false,
                callPriority = (preferences[PreferencesKeys.CALL_PRIORITY] ?: 5).coerceIn(1, 5),
                smsPriority = (preferences[PreferencesKeys.SMS_PRIORITY] ?: 4).coerceIn(1, 5),
                contactCallPriority = (preferences[PreferencesKeys.CONTACT_CALL_PRIORITY] ?: 5).coerceIn(1, 5),
                contactSmsPriority = (preferences[PreferencesKeys.CONTACT_SMS_PRIORITY] ?: 5).coerceIn(1, 5)
            )
        }

    suspend fun updateNtfyServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NTFY_SERVER_URL] = url
        }
    }

    suspend fun updateNtfyTopic(topic: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NTFY_TOPIC] = topic
        }
    }

    suspend fun updateCallNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALL_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateSmsNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMS_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateNotificationListenerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_LISTENER_ENABLED] = enabled
        }
    }

    suspend fun updateCallPriority(priority: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALL_PRIORITY] = priority.coerceIn(1, 5)
        }
    }

    suspend fun updateSmsPriority(priority: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMS_PRIORITY] = priority.coerceIn(1, 5)
        }
    }

    suspend fun updateContactCallPriority(priority: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTACT_CALL_PRIORITY] = priority.coerceIn(1, 5)
        }
    }

    suspend fun updateContactSmsPriority(priority: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTACT_SMS_PRIORITY] = priority.coerceIn(1, 5)
        }
    }
}
