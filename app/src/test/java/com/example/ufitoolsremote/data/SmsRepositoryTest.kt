package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.SmsMessage
import com.example.ufitoolsremote.network.UfiApiClient
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: SmsRepository
    private lateinit var config: ConnectionConfig

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiClient = UfiApiClient(
            httpClient = OkHttpClient(),
            clock = { 1718438543772 }
        )
        repository = SmsRepository(apiClient)
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
    fun fetchSms_decodesBase64Utf8Content() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "messages": [
                    {
                      "id": "101",
                      "number": "10086",
                      "content": "5rWL6K+V55+t5L+h",
                      "date": "2026,06,08,23,59,01,+32",
                      "tag": "1"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.fetchSms(config)

        assertTrue(result is ApiResult.Success)
        val messages = (result as ApiResult.Success).value
        assertEquals(1, messages.size)
        assertEquals("测试短信", messages.first().content)
        assertTrue(messages.first().isUnread)
    }

    @Test
    fun sendSms_encodesMessageBodyAsUtf16BeHexAndAddsAd() = runTest {
        enqueueSuccessfulSession()
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        enqueueLogout()

        val result = repository.sendSms(config, "10086", "测试SMS")

        assertTrue(result is ApiResult.Success)
        repeat(5) { server.takeRequest() }
        val sendRequest = server.takeRequest()
        val sendBody = sendRequest.body.readUtf8()
        assertEquals("SID=abc", sendRequest.getHeader("Cookie"))
        assertTrue(sendBody.contains("goformId=SEND_SMS"))
        assertTrue(sendBody.contains("Number=10086"))
        assertTrue(sendBody.contains("MessageBody=6d4b8bd50053004d0053"))
        assertTrue(sendBody.contains("AD="))
    }

    @Test
    fun deleteSms_usesRawIdFirstWithAdAndCookie() = runTest {
        enqueueSuccessfulSession()
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        enqueueLogout()

        val result = repository.deleteSms(config, "101")

        assertTrue(result is ApiResult.Success)
        repeat(5) { server.takeRequest() }
        val deleteRequest = server.takeRequest()
        val deleteBody = deleteRequest.body.readUtf8()
        assertEquals("SID=abc", deleteRequest.getHeader("Cookie"))
        assertTrue(deleteBody.contains("goformId=DELETE_SMS"))
        assertTrue(deleteBody.contains("msg_id=101"))
        assertFalse(deleteBody.contains("msg_id=101%3B"))
        assertTrue(deleteBody.contains("notCallback=true"))
        assertTrue(deleteBody.contains("AD="))
    }

    @Test
    fun deleteSms_ignoresAdminPasswordSyncFailure() = runTest {
        server.enqueue(MockResponse().setBody("""{"LD":"LDVALUE"}"""))
        server.enqueue(
            MockResponse()
                .setBody("""{"result":"success"}""")
                .addHeader("kano-cookie", "SID=abc; Path=/")
        )
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not found"}"""))
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        enqueueLogout()

        val result = repository.deleteSms(config, "101")

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun deleteSms_fallsBackToSemicolonIdOnDeviceFailure() = runTest {
        enqueueSuccessfulSession()
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"failure"}"""))
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        enqueueLogout()

        val result = repository.deleteSms(config, "101")

        assertTrue(result is ApiResult.Success)
        repeat(5) { server.takeRequest() }
        val firstDelete = server.takeRequest().body.readUtf8()
        repeat(2) { server.takeRequest() }
        val secondDelete = server.takeRequest().body.readUtf8()
        assertTrue(firstDelete.contains("msg_id=101"))
        assertFalse(firstDelete.contains("msg_id=101%3B"))
        assertTrue(secondDelete.contains("msg_id=101%3B"))
    }

    @Test
    fun markRead_usesSetReadPayloadWithoutTag() = runTest {
        enqueueSuccessfulSession()
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        enqueueLogout()

        val result = repository.markRead(config, "101")

        assertTrue(result is ApiResult.Success)
        repeat(5) { server.takeRequest() }
        val readRequest = server.takeRequest()
        val readBody = readRequest.body.readUtf8()
        assertTrue(readBody.contains("goformId=SET_MSG_READ"))
        assertTrue(readBody.contains("msg_id=101"))
        assertFalse(readBody.contains("tag=0"))
        assertTrue(readBody.contains("AD="))
    }

    @Test
    fun markUnreadMessagesRead_reusesOneSessionAndReportsOnlySuccessfulIds() = runTest {
        enqueueSuccessfulSession()
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
        enqueueAd()
        server.enqueue(MockResponse().setResponseCode(401))
        enqueueLogout()

        val result = repository.markUnreadMessagesRead(
            config,
            listOf(
                smsMessage(id = "101", isUnread = true),
                smsMessage(id = "102", isUnread = true),
                smsMessage(id = "103", isUnread = false)
            )
        )

        assertTrue(result is ApiResult.Success)
        val batch = (result as ApiResult.Success).value
        assertEquals(setOf("101"), batch.successfulIds)
        assertEquals(setOf("102"), batch.failedIds)

        val requestBodies = List(server.requestCount) { server.takeRequest().body.readUtf8() }
        assertEquals(
            1,
            requestBodies.count { body -> body.split("&").contains("goformId=LOGIN") }
        )
        assertEquals(
            2,
            requestBodies.count { it.contains("goformId=SET_MSG_READ") }
        )
    }

    @Test
    fun markUnreadMessagesRead_withNoUnreadMessagesDoesNotOpenSession() = runTest {
        val result = repository.markUnreadMessagesRead(
            config,
            listOf(smsMessage(id = "101", isUnread = false))
        )

        assertTrue(result is ApiResult.Success)
        val batch = (result as ApiResult.Success).value
        assertTrue(batch.successfulIds.isEmpty())
        assertTrue(batch.failedIds.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun deleteSms_loginFailureKeepsSpecificError() = runTest {
        server.enqueue(MockResponse().setBody("""{"LD":"LDVALUE"}"""))
        server.enqueue(MockResponse().setBody("""{"result":"3"}"""))
        server.enqueue(MockResponse().setBody("""{"result":"3"}"""))

        val result = repository.deleteSms(config, "101")

        assertTrue(result is ApiResult.DeviceError)
        assertTrue((result as ApiResult.DeviceError).message.contains("原厂管理员密码"))
    }

    private fun enqueueSuccessfulSession() {
        server.enqueue(MockResponse().setBody("""{"LD":"LDVALUE"}"""))
        server.enqueue(
            MockResponse()
                .setBody("""{"result":"success"}""")
                .addHeader("kano-cookie", "SID=abc; Path=/")
        )
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
    }

    private fun enqueueAd() {
        server.enqueue(MockResponse().setBody("""{"wa_inner_version":"WA","cr_version":"CR"}"""))
        server.enqueue(MockResponse().setBody("""{"RD":"RDVALUE"}"""))
    }

    private fun enqueueLogout() {
        enqueueAd()
        server.enqueue(MockResponse().setBody("""{"result":"success"}"""))
    }

    private fun smsMessage(id: String, isUnread: Boolean) = SmsMessage(
        id = id,
        number = "10086",
        content = "message-$id",
        rawContent = "message-$id",
        date = "2026,06,08,23,59,01,+32",
        tag = if (isUnread) "1" else "0",
        isUnread = isUnread,
        isFailed = false
    )
}
