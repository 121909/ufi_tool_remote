package com.example.ufitoolsremote.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSchedulerTest {
    @Test
    fun refreshIsNotScheduledWithoutWidgetInstances() {
        assertFalse(shouldScheduleWidgetRefresh(activeWidgetCount = 0))
    }

    @Test
    fun refreshIsScheduledWhenAWidgetExists() {
        assertTrue(shouldScheduleWidgetRefresh(activeWidgetCount = 1))
    }
}
