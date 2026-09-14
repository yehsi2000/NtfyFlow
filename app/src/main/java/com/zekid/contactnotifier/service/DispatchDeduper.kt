package com.zekid.contactnotifier.service

/**
 * Guards against double-dispatch when the same message arrives through two
 * paths (e.g. SMS_RECEIVED broadcast + [MessageNotificationListener]).
 * Keys are normalized so "+82 10-..." and "010-..." collapse to one entry.
 */
object DispatchDeduper {
    private const val WINDOW_MS = 120_000L
    private val recent = LinkedHashMap<String, Long>()

    @Synchronized
    fun tryMark(sender: String, snippet: String): Boolean {
        val now = System.currentTimeMillis()
        val iterator = recent.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value > WINDOW_MS) iterator.remove()
        }
        val key = normalizePhone(sender) + "|" + snippet
        if (recent.containsKey(key)) return false
        recent[key] = now
        return true
    }

    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return raw.trim().lowercase()
        return if (digits.startsWith("82") && digits.length > 10) {
            "0" + digits.drop(2)
        } else {
            digits
        }
    }

    fun snippetOf(body: String): String =
        if (body.length > 100) body.take(100) + "..." else body
}
