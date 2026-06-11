package com.example.ufitoolsremote.network

import org.junit.Assert.assertEquals
import org.junit.Test

class CryptoUtilsTest {
    @Test
    fun kanoSignature_matchesKnownValue() {
        val signature = CryptoUtils.kanoSignature(
            method = "GET",
            path = "/api/need_token",
            timestampMillis = 1718438543772
        )

        assertEquals(
            "b1aabe57ed6d47b03fe9bd6bc9b0b4be9667e154df4fa941d26d46fe2e0e5720",
            signature
        )
    }

    @Test
    fun sha256Hex_matchesKnownTokenValue() {
        assertEquals(
            "14f8f4bb8c0e79a02670a5fea5682da717a5b3d3dc7b1706f7a4bab9afae18c2",
            CryptoUtils.sha256Hex("abc12345")
        )
    }

    @Test
    fun sha256HexUpper_matchesFactoryGoformStyle() {
        assertEquals(
            "8C6976E5B5410415BDE908BD4DEE15DFB167A9C873FC4BB8A81F6F2AB448A918",
            CryptoUtils.sha256HexUpper("admin")
        )
    }
}
