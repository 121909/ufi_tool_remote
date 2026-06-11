package com.example.ufitoolsremote.network

import java.util.Base64

object SmsCodec {
    fun decodeBase64Utf8(value: String): String {
        val padding = (4 - value.length % 4) % 4
        val padded = value + "=".repeat(padding)
        return Base64.getDecoder().decode(padded).toString(Charsets.UTF_8)
    }

    fun encodeUtf16BeHex(text: String): String {
        val out = StringBuilder(text.length * 4)
        var i = 0
        while (i < text.length) {
            val codePoint = Character.codePointAt(text, i)
            if (codePoint <= 0xFFFF) {
                appendCodeUnit(out, codePoint)
            } else {
                val chars = Character.toChars(codePoint)
                appendCodeUnit(out, chars[0].code)
                appendCodeUnit(out, chars[1].code)
            }
            i += Character.charCount(codePoint)
        }
        return out.toString()
    }

    private fun appendCodeUnit(out: StringBuilder, codeUnit: Int) {
        out.append(((codeUnit shr 8) and 0xFF).toString(16).padStart(2, '0'))
        out.append((codeUnit and 0xFF).toString(16).padStart(2, '0'))
    }
}
