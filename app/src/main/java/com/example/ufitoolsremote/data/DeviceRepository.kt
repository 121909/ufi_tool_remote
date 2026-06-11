package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.DeviceInfo
import com.example.ufitoolsremote.model.StorageInfo
import com.example.ufitoolsremote.network.UfiApiClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class DeviceRepository(private val api: UfiApiClient) {
    suspend fun fetchDeviceInfo(config: ConnectionConfig): ApiResult<DeviceInfo> {
        val base = when (val result = api.getJson(config, "/api/baseDeviceInfo")) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }

        val cmd = listOf(
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

        val goform = when (val result = api.getJson(config, "/api/goform/goform_get_cmd_process?multi_data=1&isTest=false&cmd=$cmd&_=${System.currentTimeMillis()}")) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }

        val monthlyRxBytes = goform.long("monthly_rx_bytes")
        val monthlyTxBytes = goform.long("monthly_tx_bytes")

        return ApiResult.Success(
            DeviceInfo(
                appVersion = base.string("app_ver"),
                appVersionCode = base.string("app_ver_code"),
                model = base.string("model"),
                battery = firstNonBlank(goform.string("battery_value"), goform.string("battery_vol_percent"), base.string("battery")),
                cpuTemp = base.string("cpu_temp"),
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
                signal = firstNonBlank(goform.string("Z5g_rsrp"), goform.string("lte_rsrp"), goform.string("network_rssi"), goform.string("rssi")),
                networkType = goform.string("network_type"),
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
                lteBand = prefixedBand("B", goform.string("Lte_bands")),
                lteBandwidth = goform.string("Lte_bands_widths"),
                lteCellId = goform.string("Lte_cell_id"),
                ltePci = goform.string("Lte_pci"),
                lteFrequency = goform.string("Lte_fcn"),
                nrBand = prefixedBand("N", goform.string("Nr_bands")),
                nrBandwidth = goform.string("Nr_bands_widths"),
                nrCellId = goform.string("Nr_cell_id"),
                nrPci = goform.string("Nr_pci"),
                nrFrequency = goform.string("Nr_fcn"),
                clientIp = base.string("client_ip")
            )
        )
    }

    private fun JsonObject.string(key: String): String? = this[key].stringOrNull()?.takeUnless { it == "null" || it.isBlank() }
    private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
    private fun JsonObject.double(key: String): String? = this[key]?.jsonPrimitive?.doubleOrNull?.let { "%.1f%%".format(it) }
    private fun JsonElement?.stringOrNull(): String? = this?.jsonPrimitive?.contentOrNull

    private fun firstNonBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }

    private fun prefixedBand(prefix: String, value: String?): String? {
        val band = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (band.startsWith(prefix, ignoreCase = true)) band else "$prefix$band"
    }

    private fun sumNullable(vararg values: Long?): Long? {
        return if (values.all { it == null }) null else values.sumOf { it ?: 0L }
    }
}
