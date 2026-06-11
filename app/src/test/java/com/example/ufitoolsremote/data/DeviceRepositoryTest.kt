package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.RadioAccessTechnology
import com.example.ufitoolsremote.network.UfiApiClient
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: DeviceRepository
    private lateinit var config: ConnectionConfig

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = DeviceRepository(
            UfiApiClient(
                httpClient = OkHttpClient(),
                clock = { 1718438543772 }
            )
        )
        config = ConnectionConfig(
            baseUrl = server.url("/").toString().trimEnd('/'),
            ufiToken = "abc12345",
            adminPassword = "admin"
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchDeviceInfo_parsesExtendedRealtimeFields() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "app_ver": "3.1.5",
                  "app_ver_code": "315",
                  "model": "UFI X",
                  "battery": "80",
                  "cpu_temp": 42000,
                  "daily_data": 1073741824,
                  "monthly_data": 2147483648,
                  "client_ip": "192.168.0.8"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "battery_value": "88",
                  "network_type": "20",
                  "network_provider": "CMCC",
                  "wan_ipaddr": "10.10.10.2",
                  "ipv6_wan_ipaddr": "2409:8a00::1",
                  "lan_ipaddr": "192.168.0.1",
                  "wifi_access_sta_num": "3",
                  "realtime_rx_thrpt": "1024",
                  "realtime_tx_thrpt": "2048",
                  "monthly_rx_bytes": "1048576",
                  "monthly_tx_bytes": 2097152,
                  "Lte_bands": "3",
                  "Lte_bands_widths": "20MHz",
                  "Lte_cell_id": "12345",
                  "Lte_pci": "321",
                  "Lte_fcn": "1850",
                  "Nr_bands": "78",
                  "Nr_bands_widths": "100MHz",
                  "Nr_cell_id": "67890",
                  "Nr_pci": "654",
                  "Nr_fcn": "636666"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "Nr_snr": "20",
                  "nr_rsrq": "-9",
                  "Lte_snr": "12"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "nr5g_action_channel": "636667",
                  "nr5g_rssi": "-70",
                  "lte_ca_pcell_arfcn": "1851"
                }
                """.trimIndent()
            )
        )

        val result = repository.fetchDeviceInfo(config)

        assertTrue(result is ApiResult.Success)
        val info = (result as ApiResult.Success).value
        assertEquals("88", info.battery)
        assertEquals("42.0 °C", info.cpuTemp)
        assertEquals(1073741824L, info.dailyData)
        assertEquals(2147483648L, info.monthlyData)
        assertEquals("10.10.10.2", info.ipv4)
        assertEquals("2409:8a00::1", info.ipv6)
        assertEquals("3", info.connectedDevices)
        assertEquals(1048576L, info.monthlyRxBytes)
        assertEquals(2097152L, info.monthlyTxBytes)
        assertEquals(3145728L, info.monthlyTotalBytes)
        assertEquals("5G", info.networkType)
        assertEquals(RadioAccessTechnology.NR, info.radioAccessTechnology)
        assertEquals("B3", info.lteBand)
        assertEquals("20MHz", info.lteBandwidth)
        assertEquals("12345", info.lteCellId)
        assertEquals("321", info.ltePci)
        assertEquals("1850", info.lteFrequency)
        assertEquals("12", info.lteSinr)
        assertEquals("N78", info.nrBand)
        assertEquals("100MHz", info.nrBandwidth)
        assertEquals("67890", info.nrCellId)
        assertEquals("654", info.nrPci)
        assertEquals("636666", info.nrFrequency)
        assertEquals("20", info.nrSinr)
        assertEquals("-9", info.nrRsrq)
        assertEquals("-70", info.nrRssi)

        assertEquals("/api/baseDeviceInfo", server.takeRequest().path)
        val goformPath = server.takeRequest().path.orEmpty()
        assertTrue(goformPath.contains("wan_ipaddr"))
        assertTrue(goformPath.contains("ipv6_wan_ipaddr"))
        assertTrue(goformPath.contains("wifi_access_sta_num"))
        assertTrue(goformPath.contains("Lte_bands"))
        assertTrue(goformPath.contains("Nr_cell_id"))
        val signalPath = server.takeRequest().path.orEmpty()
        assertTrue(signalPath.contains("Nr_snr"))
        val aliasPath = server.takeRequest().path.orEmpty()
        assertTrue(aliasPath.contains("nr5g_action_channel"))
    }

    @Test
    fun fetchDeviceInfo_missingExtendedFieldsRemainNull() = runTest {
        server.enqueue(MockResponse().setBody("""{}"""))
        server.enqueue(MockResponse().setBody("""{}"""))
        server.enqueue(MockResponse().setBody("""{}"""))
        server.enqueue(MockResponse().setBody("""{}"""))

        val result = repository.fetchDeviceInfo(config)

        assertTrue(result is ApiResult.Success)
        val info = (result as ApiResult.Success).value
        assertNull(info.ipv4)
        assertNull(info.ipv6)
        assertNull(info.cpuTemp)
        assertNull(info.radioAccessTechnology)
        assertNull(info.connectedDevices)
        assertNull(info.monthlyRxBytes)
        assertNull(info.monthlyTxBytes)
        assertNull(info.monthlyTotalBytes)
        assertNull(info.lteBand)
        assertNull(info.lteBandwidth)
        assertNull(info.lteCellId)
        assertNull(info.ltePci)
        assertNull(info.lteFrequency)
        assertNull(info.nrBand)
        assertNull(info.nrBandwidth)
        assertNull(info.nrCellId)
        assertNull(info.nrPci)
        assertNull(info.nrFrequency)
    }
}
