package com.example.ufitoolsremote.util

object SmsContentUtils {
    private val linkRegex = Regex("""https?://[^\s，。；、]+""", RegexOption.IGNORE_CASE)
    private val codeRegex = Regex("""(?<!\d)\d{4,8}(?!\d)""")
    private val codeContextRegex = Regex(
        """验证码|校验码|动态码|确认码|安全码|登录码|短信码|取件码|code|otp|verification|verify|security""",
        RegexOption.IGNORE_CASE
    )

    fun extractLinks(content: String): List<String> {
        return linkRegex.findAll(content)
            .map { it.value.trimEnd('.', ',', ';', ')', ']', '}', '。', '，', '；') }
            .distinct()
            .toList()
    }

    fun extractVerificationCode(content: String): String? {
        return codeRegex.findAll(content)
            .firstOrNull { match ->
                val start = (match.range.first - CONTEXT_RADIUS).coerceAtLeast(0)
                val end = (match.range.last + 1 + CONTEXT_RADIUS).coerceAtMost(content.length)
                codeContextRegex.containsMatchIn(content.substring(start, end))
            }
            ?.value
    }

    private const val CONTEXT_RADIUS = 18
}
