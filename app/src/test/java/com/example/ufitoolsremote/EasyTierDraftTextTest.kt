package com.example.ufitoolsremote

import org.junit.Assert.assertEquals
import org.junit.Test

class EasyTierDraftTextTest {
    @Test
    fun toEditableLines_preservesTrailingNewline() {
        val input = "tcp://10.1.1.5:11010\n"

        val lines = input.toEditableLines()

        assertEquals(listOf("tcp://10.1.1.5:11010", ""), lines)
        assertEquals(input, lines.joinToString("\n"))
    }

    @Test
    fun toEditableLines_preservesWhitespaceWhileEditing() {
        val input = " tcp://first:11010 \n tcp://second:11010"

        assertEquals(
            listOf(" tcp://first:11010 ", " tcp://second:11010"),
            input.toEditableLines()
        )
    }

    @Test
    fun toEditableLines_normalizesCrLfForComposeState() {
        val input = "tcp://first:11010\r\ntcp://second:11010"

        assertEquals(
            listOf("tcp://first:11010", "tcp://second:11010"),
            input.toEditableLines()
        )
    }
}
