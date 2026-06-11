package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.DeviceInfo
import com.example.ufitoolsremote.model.RadioAccessTechnology
import com.example.ufitoolsremote.model.StorageInfo
import com.example.ufitoolsremote.network.UfiApiClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Locale

class DeviceRepository(private val api: UfiApiClient) {
    suspend fun fetchDeviceInfo(config: ConnectionConfig): ApiResult<DeviceInfo> {
        val base = when (val result = api.getJson(config, "/api/baseDeviceInfo")) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }

        val baseCmd = listOf(
            "usb_port_switch",
            "battery_charging",
            "sms_received_flag",
            "sms_unread_num",
            "sms_sim_unread_num",
            "sim_msisdn",
            "battery_value",
            "battery_vol_percent",
            "network_signalbar",
            "network_rssi",
            "cr_version",
            "iccid",
            "imei",
            "imsi",
            "wan_ipaddr",
            "ipv4_wan_ipaddr",
            "pdp_addr",
            "ipv6_wan_ipaddr",
            "lan_ipaddr",
            "mac_address",
            "msisdn",
            "network_information",
            "Lte_ca_status",
            "rssi",
            "Z5g_rsrp",
            "lte_rsrp",
            "Lte_bands",
            "Lte_bands_widths",
            "Lte_cell_id",
            "Lte_pci",
            "Lte_fcn",
            "Nr_bands",
            "Nr_bands_widths",
            "Nr_cell_id",
            "Nr_pci",
            "Nr_fcn",
            "wifi_access_sta_num",
            "realtime_rx_thrpt",
            "realtime_tx_thrpt",
            "monthly_rx_bytes",
            "monthly_tx_bytes",
            "network_type",
            "network_provider",
            "ppp_status"
        ).joinToString(",")

        val baseGoform = when (val result = api.getJson(config, "/api/goform/goform_get_cmd_process?multi_data=1&isTest=false&cmd=$baseCmd&_=${System.currentTimeMillis()}")) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }

        val signalCmd = listOf(
            "Nr_rsrp",
            "nr_rsrp",
            "Nr_snr",
            "nr_snr",
            "Nr_rsrq",
            "nr_rsrq",
            "Nr_rssi",
            "nr_rssi",
            "Lte_rsrp",
            "Lte_snr",
            "lte_snr",
            "Lte_rsrq",
            "lte_rsrq",
            "Lte_rssi",
            "lte_rssi"
        ).joinToString(",")

        val aliasCmd = listOf(
            "lte_band",
            "lte_ca_pcell_band",
            "lte_bandwidth",
            "lte_ca_pcell_bandwidth",
            "lte_cell_id",
            "lte_ca_pcell_cell_id",
            "lte_pci",
            "lte_ca_pcell_pci",
            "lte_fcn",
            "lte_earfcn",
            "lte_ca_pcell_arfcn",
            "nr_band",
            "nr5g_action_band",
            "nr5g_band",
            "nr5g_action_nsa_band",
            "nr5g_action_sa_band",
            "nr_bandwidth",
            "nr5g_bandwidth",
            "nr5g_action_bandwidth",
            "nr_cell_id",
            "nr5g_cell_id",
            "nr5g_action_cell_id",
            "nr_pci",
            "nr5g_pci",
            "nr5g_action_pci",
            "nr_fcn",
            "nr_earfcn",
            "nr5g_action_channel",
            "nr5g_action_earfcn",
            "nr5g_rsrp",
            "nr5g_rsrq",
            "nr5g_snr",
            "nr5g_rssi"
        ).joinToString(",")

        val goform = mergeOptionalGoform(config, baseGoform, signalCmd, aliasCmd)

        val monthlyRxBytes = goform.long("monthly_rx_bytes")
        val monthlyTxBytes = goform.long("monthly_tx_bytes")
        val networkTypeRaw = goform.string("network_type")
        val radioAccessTechnology = radioAccessTechnology(networkTypeRaw)
        val lteRsrp = firstNonBlank(goform.string("lte_rsrp"), goform.string("Lte_rsrp"))
        val lteSinr = firstNonBlank(goform.string("Lte_snr"), goform.string("lte_snr"))
        val lteRsrq = firstNonBlank(goform.string("lte_rsrq"), goform.string("Lte_rsrq"))
        val lteRssi = firstNonBlank(goform.string("lte_rssi"), goform.string("Lte_rssi"))
        val nrRsrp = firstNonBlank(
            goform.string("Z5g_rsrp"),
            goform.string("Nr_rsrp"),
            goform.string("nr_rsrp"),
            goform.string("nr5g_rsrp")
        )
        val nrSinr = firstNonBlank(goform.string("Nr_snr"), goform.string("nr_snr"), goform.string("nr5g_snr"))
        val nrRsrq = firstNonBlank(goform.string("nr_rsrq"), goform.string("Nr_rsrq"), goform.string("nr5g_rsrq"))
        val nrRssi = firstNonBlank(goform.string("nr_rssi"), goform.string("Nr_rssi"), goform.string("nr5g_rssi"))

        return ApiResult.Success(
            DeviceInfo(
                appVersion = base.string("app_ver"),
                appVersionCode = base.string("app_ver_code"),
                model = base.string("model"),
                battery = firstNonBlank(goform.string("battery_value"), goform.string("battery_vol_percent"), base.string("battery")),
                cpuTemp = formatTemperature(base.string("cpu_temp")),
                cpuUsage = base.double("cpu_usage"),
                memUsage = base.double("mem_usage"),
                internalStorage = StorageInfo(
                    usedBytes = base.long("internal_used_storage"),
                    totalBytes = base.long("internal_total_storage"),
                    availableBytes = base.long("internal_available_storage")
                ),
                externalStorage = StorageInfo(
                    usedBytes = base.long("external_used_storage"),
                    totalBytes = base.long("external_total_storage"),
                    availableBytes = base.long("external_available_storage")
                ),
                dailyData = base.long("daily_data"),
                monthlyData = base.long("monthly_data"),
                imei = goform.string("imei"),
                imsi = goform.string("imsi"),
                iccid = goform.string("iccid"),
                signal = firstNonBlank(nrRsrp, lteRsrp, goform.string("network_rssi"), goform.string("rssi")),
                networkType = displayNetworkType(networkTypeRaw),
                radioAccessTechnology = radioAccessTechnology,
                provider = goform.string("network_provider"),
                ipv4 = firstNonBlank(goform.string("wan_ipaddr"), goform.string("ipv4_wan_ipaddr"), goform.string("pdp_addr")),
                ipv6 = goform.string("ipv6_wan_ipaddr"),
                lanIp = goform.string("lan_ipaddr"),
                mac = goform.string("mac_address"),
                rxRate = goform.string("realtime_rx_thrpt"),
                txRate = goform.string("realtime_tx_thrpt"),
                monthlyRxBytes = monthlyRxBytes,
                monthlyTxBytes = monthlyTxBytes,
                monthlyTotalBytes = sumNullable(monthlyRxBytes, monthlyTxBytes),
                connectedDevices = goform.string("wifi_access_sta_num"),
                lteRsrp = lteRsrp,
                lteSinr = lteSinr,
                lteRsrq = lteRsrq,
                lteRssi = lteRssi,
                lteBand = prefixedBand("B", firstNonBlank(goform.string("Lte_bands"), goform.string("lte_band"), goform.string("lte_ca_pcell_band"))),
                lteBandwidth = firstNonBlank(goform.string("Lte_bands_widths"), goform.string("lte_bandwidth"), goform.string("lte_ca_pcell_bandwidth")),
                lteCellId = firstNonBlank(goform.string("Lte_cell_id"), goform.string("lte_cell_id"), goform.string("lte_ca_pcell_cell_id")),
                ltePci = firstNonBlank(goform.string("Lte_pci"), goform.string("lte_pci"), goform.string("lte_ca_pcell_pci")),
                lteFrequency = firstNonBlank(goform.string("Lte_fcn"), goform.string("lte_fcn"), goform.string("lte_earfcn"), goform.string("lte_ca_pcell_arfcn")),
                nrRsrp = nrRsrp,
                nrSinr = nrSinr,
                nrRsrq = nrRsrq,
                nrRssi = nrRssi,
                nrBand = prefixedBand("N", firstNonBlank(goform.string("Nr_bands"), goform.string("nr_band"), goform.string("nr5g_action_band"), goform.string("nr5g_band"), goform.string("nr5g_action_nsa_band"), goform.string("nr5g_action_sa_band"))),
                nrBandwidth = firstNonBlank(goform.string("Nr_bands_widths"), goform.string("nr_bandwidth"), goform.string("nr5g_bandwidth"), goform.string("nr5g_action_bandwidth")),
                nrCellId = firstNonBlank(goform.string("Nr_cell_id"), goform.string("nr_cell_id"), goform.string("nr5g_cell_id"), goform.string("nr5g_action_cell_id")),
                nrPci = firstNonBlank(goform.string("Nr_pci"), goform.string("nr_pci"), goform.string("nr5g_pci"), goform.string("nr5g_action_pci")),
                nrFrequency = firstNonBlank(goform.string("Nr_fcn"), goform.string("nr_fcn"), goform.string("nr_earfcn"), goform.string("nr5g_action_channel"), goform.string("nr5g_action_earfcn")),
                clientIp = base.string("client_ip")
            )
        )
    }

    private suspend fun mergeOptionalGoform(
        config: ConnectionConfig,
        base: JsonObject,
        vararg commands: String
    ): JsonObject {
        return commands.fold(base) { merged, command ->
            when (val result = api.getJson(config, "/api/goform/goform_get_cmd_process?multi_data=1&isTest=false&cmd=$command&_=${System.currentTimeMillis()}")) {
                is ApiResult.Success -> JsonObject(result.value + merged)
                is ApiResult.Unauthorized,
                is ApiResult.NetworkError,
                is ApiResult.ParseError,
                is ApiResult.DeviceError -> merged
            }
        }
    }

    private fun JsonObject.string(key: String): String? = this[key].stringOrNull()?.takeUnless { it == "null" || it.isBlank() }
    private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
    private fun JsonObject.double(key: String): String? = this[key]?.jsonPrimitive?.doubleOrNull?.let { "%.1f%%".format(it) }
    private fun JsonElement?.stringOrNull(): String? = this?.jsonPrimitive?.contentOrNull

    private fun firstNonBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }

    private fun radioAccessTechnology(networkType: String?): RadioAccessTechnology? {
        val value = networkType?.trim()?.lowercase(Locale.US) ?: return null
        return when {
            value == "20" || value.contains("5g") || value.contains("nr") -> RadioAccessTechnology.NR
            value == "13" || value.contains("4g") || value.contains("lte") -> RadioAccessTechnology.LTE
            else -> null
        }
    }

    private fun displayNetworkType(networkType: String?): String? {
        val value = networkType?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return when (radioAccessTechnology(value)) {
            RadioAccessTechnology.NR -> "5G"
            RadioAccessTechnology.LTE -> "4G"
            null -> value
        }
    }

    private fun formatTemperature(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (raw.contains("°C", ignoreCase = true) || raw.contains("℃")) return raw
        val number = Regex("""-?\d+(?:\.\d+)?""").find(raw)?.value?.toDoubleOrNull() ?: return raw
        if (number < 0) return null
        val digitCount = raw.count { it.isDigit() }
        val celsius = when {
            number <= 150.0 -> number
            digitCount <= 4 && number / 10.0 in 0.0..150.0 -> number / 10.0
            number / 1000.0 in 0.0..150.0 -> number / 1000.0
            number / 10.0 in 0.0..150.0 -> number / 10.0
            else -> number
        }
        return "%.1f °C".format(Locale.US, celsius)
    }

    private fun prefixedBand(prefix: String, value: String?): String? {
        val band = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (band.startsWith(prefix, ignoreCase = true)) band else "$prefix$band"
    }

    private fun sumNullable(vararg values: Long?): Long? {
        return if (values.all { it == null }) null else values.sumOf { it ?: 0L }
    }
}
