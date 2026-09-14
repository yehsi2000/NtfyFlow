package com.zekid.contactnotifier.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val ntfyServerUrl: String,
    val ntfyTopic: String,
    val callNotificationsEnabled: Boolean,
    val smsNotificationsEnabled: Boolean,
    val notificationListenerEnabled: Boolean
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val NTFY_SERVER_URL = stringPreferencesKey("ntfy_server_url")
        val NTFY_TOPIC = stringPreferencesKey("ntfy_topic")
        val CALL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("call_notifications_enabled")
        val SMS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("sms_notifications_enabled")
        val NOTIFICATION_LISTENER_ENABLED = booleanPreferencesKey("notification_listener_enabled")
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
                notificationListenerEnabled = preferences[PreferencesKeys.NOTIFICATION_LISTENER_ENABLED] ?: false
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
}
