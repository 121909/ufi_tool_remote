package com.example.ufitoolsremote.model

data class DeviceInfo(
    val appVersion: String? = null,
    val appVersionCode: String? = null,
    val model: String? = null,
    val battery: String? = null,
    val cpuTemp: String? = null,
    val cpuUsage: String? = null,
    val memUsage: String? = null,
    val internalStorage: StorageInfo? = null,
    val externalStorage: StorageInfo? = null,
    val dailyData: Long? = null,
    val monthlyData: Long? = null,
    val imei: String? = null,
    val imsi: String? = null,
    val iccid: String? = null,
    val signal: String? = null,
    val networkType: String? = null,
    val provider: String? = null,
    val ipv4: String? = null,
    val ipv6: String? = null,
    val lanIp: String? = null,
    val mac: String? = null,
    val rxRate: String? = null,
    val txRate: String? = null,
    val monthlyRxBytes: Long? = null,
    val monthlyTxBytes: Long? = null,
    val monthlyTotalBytes: Long? = null,
    val connectedDevices: String? = null,
    val lteBand: String? = null,
    val lteBandwidth: String? = null,
    val lteCellId: String? = null,
    val ltePci: String? = null,
    val lteFrequency: String? = null,
    val nrBand: String? = null,
    val nrBandwidth: String? = null,
    val nrCellId: String? = null,
    val nrPci: String? = null,
    val nrFrequency: String? = null,
    val clientIp: String? = null
)

data class StorageInfo(
    val usedBytes: Long? = null,
    val totalBytes: Long? = null,
    val availableBytes: Long? = null
)
