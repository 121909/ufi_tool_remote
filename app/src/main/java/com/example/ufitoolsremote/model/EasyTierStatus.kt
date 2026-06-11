package com.example.ufitoolsremote.model

data class EasyTierStatus(
    val rawJson: String = "",
    val instanceCount: Int = 0,
    val runningCount: Int = 0,
    val peerCount: Int = 0,
    val routeCount: Int = 0,
    val instances: List<EasyTierInstanceStatus> = emptyList(),
    val peers: List<EasyTierPeerStatus> = emptyList(),
    val errorMessage: String? = null,
    val updatedAtMillis: Long = 0L
) {
    val hasInstances: Boolean get() = instanceCount > 0
    val isEmptySnapshot: Boolean
        get() = rawJson.isBlank() &&
            instanceCount == 0 &&
            runningCount == 0 &&
            peerCount == 0 &&
            routeCount == 0 &&
            instances.isEmpty() &&
            peers.isEmpty() &&
            errorMessage.isNullOrBlank()
}

data class EasyTierInstanceStatus(
    val id: String,
    val name: String,
    val running: Boolean,
    val hostname: String?,
    val virtualIp: String?,
    val peerId: String?,
    val peerCount: Int,
    val routeCount: Int,
    val errorMessage: String?
)

data class EasyTierPeerStatus(
    val peerId: String,
    val hostname: String?,
    val virtualIp: String?,
    val version: String?,
    val cost: String?,
    val nextHopPeerId: String?,
    val nextHopHostname: String? = null,
    val nextHopInstanceName: String? = null,
    val nextHopVirtualIp: String? = null,
    val nextHopShortPeerId: String? = null,
    val pathLatency: String?,
    val proxyCidrs: List<String>,
    val connectionCount: Int,
    val latency: String?,
    val online: Boolean,
    val connections: List<EasyTierPeerConnectionStatus> = emptyList()
)

data class EasyTierPeerConnectionStatus(
    val connId: String,
    val tunnelType: String?,
    val localAddress: String?,
    val remoteAddress: String?,
    val resolvedRemoteAddress: String?,
    val networkName: String?,
    val features: List<String>,
    val latency: String?,
    val rxBytes: Long?,
    val txBytes: Long?,
    val rxPackets: Long?,
    val txPackets: Long?,
    val lossRate: String?,
    val isClient: Boolean?,
    val isClosed: Boolean,
    val secureAuthLevel: String?,
    val peerIdentityType: String?
)
