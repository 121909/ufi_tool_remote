package com.example.ufitoolsremote.ui

import com.example.ufitoolsremote.model.AppSettings
import com.example.ufitoolsremote.model.EasyTierSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UfiUiStateTest {
    private val persisted = EasyTierSettings(
        enabled = true,
        networkName = "ufi-net",
        socks5Port = 1080
    )

    @Test
    fun hasEasyTierDraftChanges_detectsFieldAndPortEdits() {
        val fieldEdit = stateWithDraft(persisted.copy(networkName = "edited-net"))
        val portEdit = stateWithDraft(persisted, portDraft = "1088")

        assertTrue(fieldEdit.hasEasyTierDraftChanges)
        assertTrue(portEdit.hasEasyTierDraftChanges)
    }

    @Test
    fun hasEasyTierDraftChanges_ignoresEnabledDifference() {
        val state = stateWithDraft(persisted.copy(enabled = false))

        assertFalse(state.hasEasyTierDraftChanges)
    }

    private fun stateWithDraft(
        draft: EasyTierSettings,
        portDraft: String = persisted.socks5Port.toString()
    ): UfiUiState {
        return UfiUiState(
            settings = AppSettings(easyTier = persisted),
            easyTierDraft = draft,
            easyTierSocks5PortDraft = portDraft
        )
    }
}
