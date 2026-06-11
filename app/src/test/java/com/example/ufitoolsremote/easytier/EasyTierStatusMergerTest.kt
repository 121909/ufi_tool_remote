package com.example.ufitoolsremote.easytier

import com.example.ufitoolsremote.model.EasyTierInstanceStatus
import com.example.ufitoolsremote.model.EasyTierStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EasyTierStatusMergerTest {
    @Test
    fun mergeKeepsPreviousRunningStateWhenCurrentSnapshotIsEmpty() {
        val previous = EasyTierStatus(
            runningCount = 1,
            instanceCount = 1,
            instances = listOf(runningInstance())
        )
        val current = EasyTierStatus(updatedAtMillis = 1234L)

        val merged = mergeEasyTierStatus(previous, current)

        assertEquals(1, merged.runningCount)
        assertEquals(1, merged.instanceCount)
        assertEquals(1234L, merged.updatedAtMillis)
    }

    @Test
    fun mergeKeepsPreviousRunningStateWhenCurrentReportsInstanceButNoRunningFlag() {
        val previous = EasyTierStatus(
            runningCount = 1,
            instanceCount = 1,
            instances = listOf(runningInstance())
        )
        val current = EasyTierStatus(
            instanceCount = 1,
            runningCount = 0,
            instances = listOf(runningInstance(running = false))
        )

        val merged = mergeEasyTierStatus(previous, current)

        assertEquals(1, merged.runningCount)
        assertEquals(1, merged.instanceCount)
    }

    @Test
    fun mergeUsesCurrentStateWhenItReportsRunning() {
        val previous = EasyTierStatus(
            runningCount = 0,
            instanceCount = 0
        )
        val current = EasyTierStatus(
            instanceCount = 1,
            runningCount = 1,
            instances = listOf(runningInstance())
        )

        val merged = mergeEasyTierStatus(previous, current)

        assertEquals(1, merged.runningCount)
        assertEquals(1, merged.instanceCount)
    }

    private fun runningInstance(running: Boolean = true) = EasyTierInstanceStatus(
        id = "instance-1",
        name = "home",
        running = running,
        hostname = "home",
        virtualIp = null,
        peerId = null,
        peerCount = 0,
        routeCount = 0,
        errorMessage = null
    )
}
