package com.example.ufitoolsremote.easytier

import com.example.ufitoolsremote.model.EasyTierSettings

object EasyTierConfigBuilder {
    fun validate(settings: EasyTierSettings): String? {
        if (settings.networkName.isBlank()) return "EasyTier network name is required"
        if (settings.socks5Enabled && settings.socks5Port !in 1..65535) return "SOCKS5 port must be between 1 and 65535"
        if (settings.socks5Enabled && settings.socks5Host.isBlank()) return "SOCKS5 bind address is required"
        return null
    }

    fun build(settings: EasyTierSettings): String {
        validate(settings)?.let { error(it) }
        return buildString {
            appendLine("instance_name = \"${settings.instanceName.tomlEscape()}\"")
            settings.hostname.takeIf { it.isNotBlank() }?.let {
                appendLine("hostname = \"${it.tomlEscape()}\"")
            }
            settings.virtualIpv4.takeIf { it.isNotBlank() }?.let {
                appendLine("ipv4 = \"${it.tomlEscape()}\"")
            }
            if (settings.listeners.isNotEmpty()) {
                appendLine("listeners = ${settings.listeners.tomlArray()}")
            }
            if (settings.socks5Enabled) {
                appendLine("socks5_proxy = \"socks5://${settings.socks5Host.tomlEscape()}:${settings.socks5Port}\"")
            }
            appendLine()
            appendLine("[network_identity]")
            appendLine("network_name = \"${settings.networkName.tomlEscape()}\"")
            if (settings.networkSecret.isNotBlank()) {
                appendLine("network_secret = \"${settings.networkSecret.tomlEscape()}\"")
            }
            settings.peers.forEach { peer ->
                appendLine()
                appendLine("[[peer]]")
                appendLine("uri = \"${peer.tomlEscape()}\"")
            }
            settings.proxyNetworks.forEach { cidr ->
                appendLine()
                appendLine("[[proxy_network]]")
                appendLine("cidr = \"${cidr.tomlEscape()}\"")
                appendLine("allow = [\"tcp\", \"udp\", \"icmp\"]")
            }
            appendLine()
            appendLine("[flags]")
            appendLine("no_tun = true")
            appendLine("enable_ipv6 = false")
            appendLine()
            appendLine("[console_logger]")
            appendLine("level = \"${settings.logLevel.tomlEscape()}\"")
        }
    }

    private fun List<String>.tomlArray(): String {
        return joinToString(prefix = "[", postfix = "]") { "\"${it.tomlEscape()}\"" }
    }

    private fun String.tomlEscape(): String {
        return buildString {
            this@tomlEscape.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }
}
