package com.example.ufitoolsremote.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsCodecTest {
    @Test
    fun decodeBase64Utf8_supportsChineseText() {
        assertEquals("测试短信", SmsCodec.decodeBase64Utf8("5rWL6K+V55+t5L+h"))
    }

    @Test
    fun encodeUtf16BeHex_matchesUfiToolsFrontend() {
        assertEquals("6d4b8bd50053004d0053", SmsCodec.encodeUtf16BeHex("测试SMS"))
    }
}
