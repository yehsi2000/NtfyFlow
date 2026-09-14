package com.zekid.contactnotifier.data

import android.util.Log
import com.zekid.contactnotifier.data.network.NtfyApi
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class NtfyRepository(private val settingsRepository: SettingsRepository) {

    private suspend fun getApi(): NtfyApi? {
        val settings = settingsRepository.appSettingsFlow.first()
        val baseUrl = settings.ntfyServerUrl.let { 
            if (it.endsWith("/")) it else "$it/"
        }
        
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

    suspend fun sendNotification(message: String): Boolean {
        val settings = settingsRepository.appSettingsFlow.first()
        val topic = settings.ntfyTopic
        if (topic.isBlank()) {
            Log.w("NtfyRepository", "Topic is blank, skipping notification")
            return false
        }

        val api = getApi() ?: return false

        return try {
            val response = api.sendNotification(topic, message)
            if (response.isSuccessful) {
                Log.d("NtfyRepository", "Notification sent successfully: $message")
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
