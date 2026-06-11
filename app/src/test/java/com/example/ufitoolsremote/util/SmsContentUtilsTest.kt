package com.example.ufitoolsremote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsContentUtilsTest {
    @Test
    fun extractVerificationCode_returnsCodeNearChineseKeyword() {
        val code = SmsContentUtils.extractVerificationCode("验证码 839201，5 分钟内有效")

        assertEquals("839201", code)
    }

    @Test
    fun extractVerificationCode_returnsCodeNearEnglishKeyword() {
        val code = SmsContentUtils.extractVerificationCode("Your OTP is 482913. Do not share it.")

        assertEquals("482913", code)
    }

    @Test
    fun extractVerificationCode_ignoresOrdinaryNumbersWithoutContext() {
        val code = SmsContentUtils.extractVerificationCode("账单 20260609，余额 123456，订单 778899。")

        assertNull(code)
    }

    @Test
    fun extractLinks_returnsHttpLinksWithoutTrailingChinesePunctuation() {
        val links = SmsContentUtils.extractLinks("请打开 https://example.com/a?b=1，或 http://u.fi/test。")

        assertEquals(listOf("https://example.com/a?b=1", "http://u.fi/test"), links)
    }
}
