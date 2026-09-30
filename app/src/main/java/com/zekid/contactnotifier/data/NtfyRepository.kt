package com.zekid.contactnotifier.data

import android.util.Log
import com.zekid.contactnotifier.data.network.NtfyApi
import com.zekid.contactnotifier.data.network.NtfyPublishRequest
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class NtfyRepository(private val settingsRepository: SettingsRepository) {

    private fun baseUrlOf(serverUrl: String): String =
        if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"

    private suspend fun getApi(): NtfyApi? {
        val settings = settingsRepository.appSettingsFlow.first()
        val baseUrl = baseUrlOf(settings.ntfyServerUrl)

        if (baseUrl.isBlank()) return null

        return try {
            val okHttpClient = OkHttpClient.Builder().build()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            retrofit.create(NtfyApi::class.java)
        } catch (e: Exception) {
            Log.e("NtfyRepository", "Failed to create NtfyApi", e)
            null
        }
    }

    suspend fun sendNotification(
        message: String,
        priority: Int = 3,
        title: String? = null,
        tags: String? = null
    ): Boolean {
        val settings = settingsRepository.appSettingsFlow.first()
        val topic = settings.ntfyTopic
        if (topic.isBlank()) {
            Log.w("NtfyRepository", "Topic is blank, skipping notification")
            return false
        }

        val api = getApi() ?: return false
        val request = NtfyPublishRequest(
            topic = topic,
            message = message,
            title = title,
            priority = priority.coerceIn(1, 5),
            tags = tags?.let { listOf(it) }
        )

        return try {
            val response = api.publish(baseUrlOf(settings.ntfyServerUrl), request)
            if (response.isSuccessful) {
                Log.d("NtfyRepository", "Notification sent successfully (priority=${request.priority}): $message")
                true
            } else {
                Log.e("NtfyRepository", "Failed to send notification: ${response.code()} ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("NtfyRepository", "Error sending notification", e)
            false
        }
    }
}
