package com.zekid.contactnotifier.service

import com.zekid.contactnotifier.data.ContactRepository
import com.zekid.contactnotifier.data.NtfyRepository

class NotificationDispatcher(
    private val contactRepository: ContactRepository,
    private val ntfyRepository: NtfyRepository
) {

    suspend fun dispatchCallNotification(phoneNumber: String): Boolean {
        val contactName = contactRepository.getContactName(phoneNumber)
        val message = if (contactName != null) {
            "Call from $contactName ($phoneNumber)"
        } else {
            "Call from $phoneNumber"
        }
        return ntfyRepository.sendNotification(message)
    }

    suspend fun dispatchSmsNotification(phoneNumber: String, messageSnippet: String): Boolean {
        val contactName = contactRepository.getContactName(phoneNumber)
        val sender = contactName ?: phoneNumber
        val fullMessage = "Message from $sender: $messageSnippet"
        return ntfyRepository.sendNotification(fullMessage)
    }
}
