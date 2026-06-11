package com.example.ufitoolsremote.easytier

import com.easytier.jni.EasyTierJNI
import com.example.ufitoolsremote.model.EasyTierInstanceStatus
import com.example.ufitoolsremote.model.EasyTierPeerConnectionStatus
import com.example.ufitoolsremote.model.EasyTierPeerStatus
import com.example.ufitoolsremote.model.EasyTierSettings
import com.example.ufitoolsremote.model.EasyTierStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object EasyTierRuntime {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun start(settings: EasyTierSettings): EasyTierRuntimeResult {
        val validation = EasyTierConfigBuilder.validate(settings)
        if (validation != null) return EasyTierRuntimeResult.Failure(validation)

        val config = EasyTierConfigBuilder.build(settings)
        return runCatching {
            val parseResult = EasyTierJNI.parseConfig(config)
            if (parseResult != 0) {
                return EasyTierRuntimeResult.Failure(nativeError("EasyTier config parse failed"))
            }

            val startResult = EasyTierJNI.runNetworkInstance(config)
            if (startResult != 0) {
                return EasyTierRuntimeResult.Failure(nativeError("EasyTier start failed"))
            }
            EasyTierRuntimeResult.Success
        }.getOrElse { throwable ->
            EasyTierRuntimeResult.Failure(errorMessage(throwable))
        }
    }

    fun stop(): EasyTierRuntimeResult {
        return runCatching {
            val result = EasyTierJNI.stopAllInstances()
            if (result == 0) EasyTierRuntimeResult.Success else EasyTierRuntimeResult.Failure(nativeError("EasyTier 停止失败"))
        }.getOrElse { throwable ->
            EasyTierRuntimeResult.Failure(errorMessage(throwable))
        }
    }

    fun collectStatus(maxLength: Int = 16): EasyTierStatus {
        return runCatching {
            val raw = EasyTierJNI.collectNetworkInfos(maxLength).orEmpty()
            val runningInstancesRaw = EasyTierJNI.listInstances(maxLength).orEmpty()
            parseStatus(raw, runningInstancesRaw)
        }.getOrElse { throwable ->
            EasyTierStatus(
                errorMessage = errorMessage(throwable),
                updatedAtMillis = System.currentTimeMillis()
            )
        }
    }

    private fun nativeError(fallback: String): String {
        return runCatching { EasyTierJNI.getLastError() }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun errorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnsatisfiedLinkError -> "EasyTier 原生库缺失，请确认 JNI 库已打包进 APK"
            else -> throwable.message?.takeIf { it.isNotBlank() } ?: throwable::class.java.simpleName
        }
    }

    internal fun parseStatus(raw: String, runningInstancesRaw: String? = null): EasyTierStatus {
        val fallbackInstances = parseRunningInstances(runningInstancesRaw)
        val runningKeys = fallbackInstances.flatMap { instance ->
            buildList {
                if (instance.id.isNotBlank()) add(instance.id)
                if (instance.name.isNotBlank()) add(instance.name)
            }
        }.toSet()

        if (raw.isBlank()) {
            if (fallbackInstances.isNotEmpty()) {
                return EasyTierStatus(
                    rawJson = runningInstancesRaw.orEmpty(),
                    instanceCount = fallbackInstances.size,
                    runningCount = fallbackInstances.size,
                    instances = fallbackInstances,
                    updatedAtMillis = System.currentTimeMillis()
                )
            }
            return EasyTierStatus(updatedAtMillis = System.currentTimeMillis())
        }
        val root = json.parseToJsonElement(raw).jsonObject
        val map = root.objectOrNull("map").orEmpty()
        val instances = map.map { (id, element) ->
            val obj = element.objectOrNull() ?: JsonObject(emptyMap())
            val node = obj.objectOrNull("my_node_info")
                ?: obj.objectOrNull("myNodeInfo")
                ?: JsonObject(emptyMap())
            val peers = obj.arrayOrNull("peers").orEmpty()
            val routes = obj.arrayOrNull("routes").orEmpty()
            EasyTierInstanceStatus(
                id = id,
                name = obj.stringOrNull("dev_name")
                    ?: obj.stringOrNull("devName")
                    ?: node.stringOrNull("hostname")
                    ?: id.take(8),
                running = obj.booleanOrNull("running") ?: false,
                hostname = node.stringOrNull("hostname"),
                virtualIp = node.ipLikeString("virtual_ipv4") ?: node.ipLikeString("virtualIpv4"),
                peerId = node.stringOrNull("peer_id") ?: node.stringOrNull("peerId"),
                peerCount = peers.size,
                routeCount = routes.size,
                errorMessage = obj.stringOrNull("error_msg") ?: obj.stringOrNull("errorMsg")
            )
        }
        val normalizedInstances = if (runningKeys.isNotEmpty()) {
            instances.map { instance ->
                if (runningKeys.contains(instance.id) || runningKeys.contains(instance.name)) {
                    instance.copy(running = true)
                } else {
                    instance
                }
            }
        } else {
            instances
        }
        val peers = map.values.flatMap { element ->
            val obj = element.objectOrNull() ?: return@flatMap emptyList()
            obj.arrayOrNull("peer_route_pairs").orEmpty().mapNotNull { pair ->
                peerFromPair(pair)
            }.ifEmpty {
                obj.arrayOrNull("peers").orEmpty().mapNotNull { peerFromPeerInfo(it) }
            }
        }.distinctBy { "${it.peerId}-${it.virtualIp.orEmpty()}-${it.hostname.orEmpty()}" }
        val peersWithNextHopNames = peers.withNextHopNames(normalizedInstances)

        val status = EasyTierStatus(
            rawJson = raw,
            instanceCount = normalizedInstances.size,
            runningCount = normalizedInstances.count { it.running },
            peerCount = peersWithNextHopNames.size.ifZero { normalizedInstances.sumOf { it.peerCount } },
            routeCount = normalizedInstances.sumOf { it.routeCount },
            instances = normalizedInstances,
            peers = peersWithNextHopNames,
            errorMessage = normalizedInstances.firstNotNullOfOrNull { it.errorMessage },
            updatedAtMillis = System.currentTimeMillis()
        )
        if (fallbackInstances.isNotEmpty() && status.runningCount == 0 && status.instances.isEmpty()) {
            return status.copy(
                rawJson = runningInstancesRaw.orEmpty().ifBlank { raw },
                instanceCount = fallbackInstances.size,
                runningCount = fallbackInstances.size,
                instances = fallbackInstances,
                errorMessage = null
            )
        }
        return status
    }

    private fun parseRunningInstances(raw: String?): List<EasyTierInstanceStatus> {
        if (raw.isNullOrBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyList()
        return root.mapNotNull { (name, value) ->
            val instanceId = value.primitiveString()?.takeIf { it.isNotBlank() } ?: name
            EasyTierInstanceStatus(
                id = instanceId,
                name = name,
                running = true,
                hostname = name,
                virtualIp = null,
                peerId = null,
                peerCount = 0,
                routeCount = 0,
                errorMessage = null
            )
        }
    }

    private fun peerFromPair(element: JsonElement): EasyTierPeerStatus? {
        val pair = element.objectOrNull() ?: return null
        val route = pair.objectOrNull("route")
        val peer = pair.objectOrNull("peer")
        return peerFromPeerInfo(peer, route)
    }

    private fun peerFromPeerInfo(element: JsonElement?): EasyTierPeerStatus? {
        return peerFromPeerInfo(element?.objectOrNull(), null)
    }

    private fun peerFromPeerInfo(peer: JsonObject?, route: JsonObject?): EasyTierPeerStatus? {
        val peerId = peer?.stringOrNull("peer_id")
            ?: peer?.stringOrNull("peerId")
            ?: route?.stringOrNull("peer_id")
            ?: route?.stringOrNull("peerId")
            ?: return null
        val conns = peer?.arrayOrNull("conns").orEmpty()
        val firstConn = conns.firstNotNullOfOrNull { it.objectOrNull() }
        val connections = conns.mapNotNull { connFromJson(it) }
        val latencyUs = firstConn?.objectOrNull("stats")?.longOrNull("latency_us")
            ?: firstConn?.objectOrNull("stats")?.longOrNull("latencyUs")
        val nextHopPeerId = (route?.stringOrNull("next_hop_peer_id") ?: route?.stringOrNull("nextHopPeerId"))
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != peerId.trim() }
        return EasyTierPeerStatus(
            peerId = peerId,
            hostname = route?.stringOrNull("hostname"),
            virtualIp = route?.ipLikeString("ipv4_addr") ?: route?.ipLikeString("ipv4Addr"),
            version = route?.stringOrNull("version"),
            cost = route?.stringOrNull("cost"),
            nextHopPeerId = nextHopPeerId,
            pathLatency = route?.longOrNull("path_latency")?.let { "$it ms" }
                ?: route?.longOrNull("pathLatency")?.let { "$it ms" },
            proxyCidrs = route?.arrayStrings("proxy_cidrs")
                ?: route?.arrayStrings("proxyCidrs")
                ?: emptyList(),
            connectionCount = conns.size,
            latency = latencyUs?.let { "${(it / 1000.0).formatOne()} ms" },
            online = connections.any { !it.isClosed },
            connections = connections
        )
    }

    private fun List<EasyTierPeerStatus>.withNextHopNames(
        instances: List<EasyTierInstanceStatus>
    ): List<EasyTierPeerStatus> {
        val peersById = buildMap<String, PeerDisplayInfo> {
            instances.forEach { instance ->
                instance.peerId?.trim()?.takeIf { it.isNotBlank() }?.let { peerId ->
                    put(
                        peerId,
                        PeerDisplayInfo(
                            hostname = instance.hostname,
                            instanceName = instance.name,
                            virtualIp = instance.virtualIp
                        )
                    )
                }
            }
            this@withNextHopNames.forEach { peer ->
                val peerId = peer.peerId.trim().takeIf { it.isNotBlank() } ?: return@forEach
                val info = PeerDisplayInfo(
                    hostname = peer.hostname,
                    instanceName = null,
                    virtualIp = peer.virtualIp
                )
                val existing: PeerDisplayInfo? = this[peerId]
                put(peerId, existing?.merge(info) ?: info)
            }
        }

        return map { peer ->
            val nextHopPeerId = peer.nextHopPeerId?.trim()?.takeIf { it.isNotBlank() } ?: return@map peer
            if (nextHopPeerId == peer.peerId.trim()) return@map peer.copy(nextHopPeerId = null)
            val nextHop = peersById[nextHopPeerId]
            peer.copy(
                nextHopHostname = nextHop?.hostname,
                nextHopInstanceName = nextHop?.instanceName,
                nextHopVirtualIp = nextHop?.virtualIp,
                nextHopShortPeerId = nextHopPeerId.shortPeerId()
            )
        }
    }

    private data class PeerDisplayInfo(
        val hostname: String?,
        val instanceName: String?,
        val virtualIp: String?
    ) {
        fun merge(other: PeerDisplayInfo): PeerDisplayInfo {
            return PeerDisplayInfo(
                hostname = hostname ?: other.hostname,
                instanceName = instanceName ?: other.instanceName,
                virtualIp = virtualIp ?: other.virtualIp
            )
        }
    }

    private fun connFromJson(element: JsonElement): EasyTierPeerConnectionStatus? {
        val obj = element.objectOrNull() ?: return null
        val stats = obj.objectOrNull("stats")
        val tunnel = obj.objectOrNull("tunnel")
        val latencyUs = stats?.longOrNull("latency_us") ?: stats?.longOrNull("latencyUs")
        return EasyTierPeerConnectionStatus(
            connId = obj.stringOrNull("conn_id")
                ?: obj.stringOrNull("connId")
                ?: "connection",
            tunnelType = tunnel?.stringOrNull("tunnel_type") ?: tunnel?.stringOrNull("tunnelType"),
            localAddress = tunnel?.urlString("local_addr") ?: tunnel?.urlString("localAddr"),
            remoteAddress = tunnel?.urlString("remote_addr") ?: tunnel?.urlString("remoteAddr"),
            resolvedRemoteAddress = tunnel?.urlString("resolved_remote_addr") ?: tunnel?.urlString("resolvedRemoteAddr"),
            networkName = obj.stringOrNull("network_name") ?: obj.stringOrNull("networkName"),
            features = obj.arrayStrings("features"),
            latency = latencyUs?.let { "${(it / 1000.0).formatOne()} ms" },
            rxBytes = stats?.longOrNull("rx_bytes") ?: stats?.longOrNull("rxBytes"),
            txBytes = stats?.longOrNull("tx_bytes") ?: stats?.longOrNull("txBytes"),
            rxPackets = stats?.longOrNull("rx_packets") ?: stats?.longOrNull("rxPackets"),
            txPackets = stats?.longOrNull("tx_packets") ?: stats?.longOrNull("txPackets"),
            lossRate = obj.doubleOrNull("loss_rate")?.let { "${(it * 100.0).formatOne()}%" }
                ?: obj.doubleOrNull("lossRate")?.let { "${(it * 100.0).formatOne()}%" },
            isClient = obj.booleanOrNull("is_client") ?: obj.booleanOrNull("isClient"),
            isClosed = obj.booleanOrNull("is_closed") ?: obj.booleanOrNull("isClosed") ?: false,
            secureAuthLevel = obj.stringOrNull("secure_auth_level") ?: obj.stringOrNull("secureAuthLevel"),
            peerIdentityType = obj.stringOrNull("peer_identity_type") ?: obj.stringOrNull("peerIdentityType")
        )
    }

    private fun JsonElement.objectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()
    private fun JsonObject.objectOrNull(key: String): JsonObject? = this[key]?.objectOrNull()
    private fun JsonObject.arrayOrNull(key: String): JsonArray? = runCatching { this[key]?.jsonArray }.getOrNull()
    private fun JsonObject.stringOrNull(key: String): String? = this[key]?.primitiveString()
    private fun JsonObject.booleanOrNull(key: String): Boolean? = runCatching { this[key]?.jsonPrimitive?.booleanOrNull }.getOrNull()
    private fun JsonObject.longOrNull(key: String): Long? = runCatching { this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull() }.getOrNull()
    private fun JsonObject.doubleOrNull(key: String): Double? = runCatching { this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() }.getOrNull()
    private fun JsonObject.arrayStrings(key: String): List<String> {
        return arrayOrNull(key).orEmpty().mapNotNull { it.primitiveString() }
    }

    private fun JsonObject.urlString(key: String): String? {
        val element = this[key] ?: return null
        element.primitiveString()?.let { return it }
        return element.objectOrNull()?.stringOrNull("url")
    }

    private fun JsonElement.primitiveString(): String? {
        val primitive = this as? JsonPrimitive ?: runCatching { jsonPrimitive }.getOrNull()
        return primitive?.contentOrNull
    }

    private fun JsonObject.ipLikeString(key: String): String? {
        val element = this[key] ?: return null
        element.primitiveString()?.let { return it }
        val obj = element.objectOrNull() ?: return null
        return obj.stringOrNull("address")
            ?: obj.stringOrNull("addr")
            ?: obj.stringOrNull("ip")
            ?: obj.stringOrNull("value")
    }

    private fun Int.ifZero(fallback: () -> Int): Int = if (this == 0) fallback() else this

    private fun String.shortPeerId(): String = take(8)

    private fun Double.formatOne(): String = "%.1f".format(this)
}

sealed interface EasyTierRuntimeResult {
    data object Success : EasyTierRuntimeResult
    data class Failure(val message: String) : EasyTierRuntimeResult
}
