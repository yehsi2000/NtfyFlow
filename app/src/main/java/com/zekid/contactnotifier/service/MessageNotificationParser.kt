package com.zekid.contactnotifier.service

import android.app.Notification
import android.os.Bundle

/** Reads the message payload rather than the rendered notification preview. */
internal object MessageNotificationParser {
    @Suppress("DEPRECATION")
    fun extractMessages(extras: Bundle): List<Pair<String, String>> {
        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)
                ?.toString()?.takeIf { it.isNotBlank() }
        val bundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(bundles)
        if (messages.isNotEmpty()) {
            // Standard MessagingStyle identifies the local user. Vendor payloads
            // may omit that metadata and the sender, using the title instead.
            val hasLocalUser = extras.containsKey(Notification.EXTRA_MESSAGING_PERSON)
                || extras.containsKey(Notification.EXTRA_SELF_DISPLAY_NAME)
            return messages.mapNotNull { message ->
                val person = message.senderPerson
                if (person == null && hasLocalUser) return@mapNotNull null
                val sender = person?.name?.toString()?.takeIf { it.isNotBlank() }
                    ?: title ?: return@mapNotNull null
                val body = message.text?.toString()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                sender to body
            }
        }
        val sender = title ?: return emptyList()
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map { it.toString() }?.filter { it.isNotBlank() }
                ?.joinToString("\n")?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?.toString()?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return listOf(sender to body)
    }
}
