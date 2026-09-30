package com.zekid.contactnotifier

import android.app.Notification
import android.app.Person
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zekid.contactnotifier.service.MessageNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageNotificationParserTest {
    @Test
    fun decodesBundlesAndKeepsAllIncomingBodies() {
        val sender = Person.Builder().setName("01012345678").build()
        val body = "긴 문자 본문".repeat(100)
        val style = Notification.MessagingStyle(Person.Builder().setName("나").build())
            .addMessage(body, 1L, sender)
            .addMessage("두 번째 메시지", 2L, sender)
            .addMessage("내가 보낸 메시지", 3L, null as Person?)
        val context = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        val extras = Notification.Builder(context, "test").setStyle(style).build().extras
        assertEquals(
            listOf("01012345678" to body, "01012345678" to "두 번째 메시지"),
            MessageNotificationParser.extractMessages(extras)
        )
    }

    @Test
    fun vendorMessageWithoutSenderUsesTitle() {
        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, "발신자")
            putParcelableArray(Notification.EXTRA_MESSAGES, arrayOf(Bundle().apply {
                putCharSequence("text", "채팅+ 본문")
                putLong("time", 1L)
            }))
        }
        assertEquals(listOf("발신자" to "채팅+ 본문"), MessageNotificationParser.extractMessages(extras))
    }

    @Test
    fun prefersExpandedBodyAndFallsBackToLines() {
        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, "발신자")
            putCharSequence(Notification.EXTRA_TEXT, "요약")
            putCharSequenceArray(Notification.EXTRA_TEXT_LINES, arrayOf("첫 줄", "둘째 줄"))
            putCharSequence(Notification.EXTRA_BIG_TEXT, "전체 본문")
        }
        assertEquals(listOf("발신자" to "전체 본문"), MessageNotificationParser.extractMessages(extras))
        extras.remove(Notification.EXTRA_BIG_TEXT)
        assertEquals(listOf("발신자" to "첫 줄\n둘째 줄"), MessageNotificationParser.extractMessages(extras))
    }
}
