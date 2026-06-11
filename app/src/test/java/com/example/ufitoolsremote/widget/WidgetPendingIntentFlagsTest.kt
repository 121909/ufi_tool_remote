package com.example.ufitoolsremote.widget

import android.app.PendingIntent
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPendingIntentFlagsTest {
    @Test
    fun mutableCollectionTemplate_isNotImmutableBeforeAndroid12() {
        val flags = widgetPendingIntentFlags(mutable = true, sdkInt = Build.VERSION_CODES.R)

        assertEquals(PendingIntent.FLAG_UPDATE_CURRENT, flags)
        assertFalse(flags.hasFlag(PendingIntent.FLAG_IMMUTABLE))
    }

    @Test
    fun mutableCollectionTemplate_usesMutableOnAndroid12AndNewer() {
        val flags = widgetPendingIntentFlags(mutable = true, sdkInt = Build.VERSION_CODES.S)

        assertTrue(flags.hasFlag(PendingIntent.FLAG_MUTABLE))
        assertFalse(flags.hasFlag(PendingIntent.FLAG_IMMUTABLE))
    }

    @Test
    fun regularWidgetActions_stayImmutable() {
        val flags = widgetPendingIntentFlags(mutable = false, sdkInt = Build.VERSION_CODES.S)

        assertTrue(flags.hasFlag(PendingIntent.FLAG_IMMUTABLE))
        assertFalse(flags.hasFlag(PendingIntent.FLAG_MUTABLE))
    }

    private fun Int.hasFlag(flag: Int): Boolean = this and flag == flag
}
