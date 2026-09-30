package com.zekid.contactnotifier.service

import com.zekid.contactnotifier.data.ContactRepository
import com.zekid.contactnotifier.data.NtfyRepository
import com.zekid.contactnotifier.data.SettingsRepository
import kotlinx.coroutines.flow.first

class NotificationDispatcher(
    private val contactRepository: ContactRepository,
    private val ntfyRepository: NtfyRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun dispatchCallNotification(phoneNumber: String): Boolean {
        val settings = settingsRepository.appSettingsFlow.first()
        val contactName = try {
            contactRepository.getContactName(phoneNumber)
        } catch (e: SecurityException) {
            null
        }
        val priority = if (contactName != null) {
            settings.contactCallPriority
        } else {
            settings.callPriority
        }
        val title = if (contactName != null) {
            "전화 수신: $contactName"
        } else {
            "전화 수신"
        }
        val message = if (contactName != null) {
            "Call from $contactName ($phoneNumber)"
        } else {
            "Call from $phoneNumber"
        }
        return ntfyRepository.sendNotification(message, priority, title, "telephone")
    }

    suspend fun dispatchSmsNotification(phoneNumber: String, messageSnippet: String): Boolean {
        val settings = settingsRepository.appSettingsFlow.first()
        val contactName = try {
            contactRepository.getContactName(phoneNumber)
        } catch (e: SecurityException) {
            null
        }
        val sender = contactName ?: phoneNumber
        val priority = if (contactName != null) {
            settings.contactSmsPriority
        } else {
            settings.smsPriority
        }
        val fullMessage = "Message from $sender: $messageSnippet"
        return ntfyRepository.sendNotification(fullMessage, priority, "문자 수신: $sender", "message")
    }
}
