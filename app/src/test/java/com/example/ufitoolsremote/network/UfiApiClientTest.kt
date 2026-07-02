package com.example.ufitoolsremote.network

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.UfiAccessMode
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

class UfiApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: UfiApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = UfiApiClient(
            httpClient = OkHttpClient(),
            clock = { 1718438543772 }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun needToken_addsUfiSignatureHeaders() = runTest {
        server.enqueue(MockResponse().setBody("""{"need_token":true}"""))
        val config = testConfig()

        val result = client.needToken(config)
        val request = server.takeRequest()

        assertEquals(ApiResult.Success(true), result)
        assertEquals("1718438543772", request.getHeader("kano-t"))
        assertEquals(
            "b1aabe57ed6d47b03fe9bd6bc9b0b4be9667e154df4fa941d26d46fe2e0e5720",
            request.getHeader("kano-sign")
        )
        assertEquals(
            "14f8f4bb8c0e79a02670a5fea5682da717a5b3d3dc7b1706f7a4bab9afae18c2",
            request.getHeader("Authorization")
        )
    }

    @Test
    fun unauthorizedResponse_mapsToUnauthorized() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = client.needToken(testConfig())

        assertTrue(result is ApiResult.Unauthorized)
    }

    @Test
    fun socks5Mode_proxyUnavailableReturnsNetworkErrorWithoutDirectFallback() = runTest {
        server.enqueue(MockResponse().setBody("""{"need_token":true}"""))
        val unavailablePort = ServerSocket(0).use { it.localPort }
        val config = testConfig(
            accessMode = UfiAccessMode.EasyTierSocks5,
            easyTierSocks5Host = "127.0.0.1",
            easyTierSocks5Port = unavailablePort
        )

        val result = client.needToken(config)

        assertTrue(result is ApiResult.NetworkError)
        assertTrue((result as ApiResult.NetworkError).message.contains("EasyTier SOCKS5"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun loginGoform_fallsBackFromMultiUserToLegacy() = runTest {
        server.enqueue(MockResponse().setBody("""{"LD":"LDVALUE"}"""))
        server.enqueue(MockResponse().setBody("""{"result":"3"}"""))
        server.enqueue(
            MockResponse()
                .setBody("""{"result":"success"}""")
                .addHeader("kano-cookie", "SID=abc; Path=/")
        )

        val result = client.loginGoform(testConfig(loginModePreference = LoginModePreference.MultiUserFirst))
        val ldRequest = server.takeRequest()
        val multiLoginRequest = server.takeRequest()
        val legacyLoginRequest = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals("SID=abc", (result as ApiResult.Success).value.cookie)
        assertTrue(ldRequest.path.orEmpty().contains("cmd=LD"))
        assertTrue(multiLoginRequest.body.readUtf8().contains("goformId=LOGIN_MULTI_USER"))
        assertTrue(legacyLoginRequest.body.readUtf8().contains("goformId=LOGIN"))
    }

    @Test
    fun loginGoform_defaultsToLegacyLoginWithUppercaseFactoryHash() = runTest {
        server.enqueue(MockResponse().setBody("""{"LD":"LDVALUE"}"""))
        server.enqueue(
            MockResponse()
                .setBody("""{"result":"success"}""")
                .addHeader("kano-cookie", "SID=abc; Path=/")
        )

        val result = client.loginGoform(testConfig())
        server.takeRequest()
        val loginRequest = server.takeRequest()
        val body = loginRequest.body.readUtf8()
        val expectedPassword = CryptoUtils.sha256HexUpper(CryptoUtils.sha256HexUpper("admin") + "LDVALUE")

        assertTrue(result is ApiResult.Success)
        assertTrue(body.contains("goformId=LOGIN"))
        assertTrue(body.contains("password=$expectedPassword"))
    }

    @Test
    fun loginGoform_successWithoutCookieIsDeviceError() = runTest {
        server.enqueue(MockResponse().setBody("""{"LD":"LDVALUE"}"""))
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))

        val result = client.loginGoform(testConfig())

        assertTrue(result is ApiResult.DeviceError)
        assertTrue((result as ApiResult.DeviceError).message.contains("Cookie"))
    }

    @Test
    fun updateAdminPassword_postsJsonPayload() = runTest {
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))

        val result = client.updateAdminPassword(testConfig())
        val request = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals("/api/update_admin_pwd", request.path)
        assertEquals("""{"password":"admin"}""", request.body.readUtf8())
    }

    @Test
    fun postForm_sendsCookieAndKanoCookieHeaders() = runTest {
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))

        val result = client.postForm(
            testConfig(),
            "/api/goform/goform_set_cmd_process",
            mapOf("goformId" to "DELETE_SMS"),
            "SID=abc"
        )
        val request = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals("SID=abc", request.getHeader("Cookie"))
        assertEquals("SID=abc", request.getHeader("Kano-Cookie"))
    }

    private fun testConfig(
        loginModePreference: LoginModePreference = LoginModePreference.LegacyFirst,
        accessMode: UfiAccessMode = UfiAccessMode.Direct,
        easyTierSocks5Host: String = "127.0.0.1",
        easyTierSocks5Port: Int = 1080
    ): ConnectionConfig {
        return ConnectionConfig(
            baseUrl = server.url("/").toString().trimEnd('/'),
            ufiToken = "abc12345",
            adminPassword = "admin",
            loginModePreference = loginModePreference,
            accessMode = accessMode,
            easyTierSocks5Host = easyTierSocks5Host,
            easyTierSocks5Port = easyTierSocks5Port
        )
    }
}
