package com.example.ufitoolsremote.network

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    const val REQUEST_SECRET_KEY = "minikano_kOyXz0Ciz4V7wR0IeKmJFYFQ20jd"

    fun sha256Hex(text: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .toHex()
    }

    fun sha256HexUpper(text: String): String {
        return sha256Hex(text).uppercase()
    }

    fun kanoSignature(
        secret: String = REQUEST_SECRET_KEY,
        method: String,
        path: String,
        timestampMillis: Long
    ): String {
        val raw = "minikano${method.uppercase()}$path$timestampMillis"
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacMD5"))
        val hmacBytes = mac.doFinal(raw.toByteArray(Charsets.UTF_8))
        val mid = hmacBytes.size / 2
        val part1 = hmacBytes.copyOfRange(0, mid)
        val part2 = hmacBytes.copyOfRange(mid, hmacBytes.size)
        val sha1 = sha256Bytes(part1)
        val sha2 = sha256Bytes(part2)
        return sha256Bytes(sha1 + sha2).toHex()
    }

    private fun sha256Bytes(bytes: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(bytes)
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
