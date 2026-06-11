package com.example.ufitoolsremote.easytier

import com.example.ufitoolsremote.model.EasyTierSettings
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasyTierConfigBuilderTest {
    @Test
    fun build_writesNoTunAndSocks5Proxy() {
        val config = EasyTierConfigBuilder.build(
            EasyTierSettings(
                networkName = "ufi-net",
                networkSecret = "secret",
                peers = listOf("tcp://10.1.1.5:11010"),
                listeners = listOf("tcp://0.0.0.0:11010"),
                proxyNetworks = listOf("10.1.1.0/24"),
                socks5Enabled = true,
                socks5Host = "0.0.0.0",
                socks5Port = 1088
            )
        )

        assertTrue(config.contains("socks5_proxy = \"socks5://0.0.0.0:1088\""))
        assertTrue(config.contains("[flags]"))
        assertTrue(config.contains("no_tun = true"))
        assertTrue(config.contains("[[peer]]"))
        assertTrue(config.contains("uri = \"tcp://10.1.1.5:11010\""))
        assertTrue(config.contains("[[proxy_network]]"))
        assertTrue(config.contains("cidr = \"10.1.1.0/24\""))
    }

    @Test
    fun validate_requiresNetworkName() {
        val error = EasyTierConfigBuilder.validate(EasyTierSettings(networkName = ""))

        assertTrue(error.orEmpty().contains("network name"))
    }

    @Test
    fun validate_allowsSocks5DisabledWithoutProxyPortConcern() {
        val error = EasyTierConfigBuilder.validate(
            EasyTierSettings(
                networkName = "ufi-net",
                socks5Enabled = false,
                socks5Port = 0
            )
        )

        assertNull(error)
    }
}
