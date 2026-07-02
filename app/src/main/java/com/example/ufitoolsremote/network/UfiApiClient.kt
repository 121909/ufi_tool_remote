package com.example.ufitoolsremote.network

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.UfiAccessMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class UfiApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun getJson(config: ConnectionConfig, pathWithQuery: String): ApiResult<JsonObject> {
        return executeJson(config, "GET", pathWithQuery, null, null)
    }

    suspend fun postForm(
        config: ConnectionConfig,
        pathWithQuery: String,
        form: Map<String, String>,
        kanoCookie: String? = null
    ): ApiResult<JsonObject> {
        return executeJson(config, "POST", pathWithQuery, formBody(form), kanoCookie)
    }

    suspend fun needToken(config: ConnectionConfig): ApiResult<Boolean> {
        return getJson(config, "/api/need_token").let { result ->
            when (result) {
                is ApiResult.Success -> ApiResult.Success(result.value["need_token"]?.jsonPrimitive?.contentOrNull == "true")
                is ApiResult.Unauthorized -> result
                is ApiResult.NetworkError -> result
                is ApiResult.ParseError -> result
                is ApiResult.DeviceError -> result
            }
        }
    }

    suspend fun loginGoform(config: ConnectionConfig): ApiResult<GoformSession> = withContext(Dispatchers.IO) {
        val ld = when (val ldResult = getGoformValue(config, "LD", null)) {
            is ApiResult.Success -> ldResult.value["LD"].orEmpty()
            is ApiResult.Unauthorized -> return@withContext ldResult
            is ApiResult.NetworkError -> return@withContext ldResult
            is ApiResult.ParseError -> return@withContext ldResult
            is ApiResult.DeviceError -> return@withContext ldResult
        }
        if (ld.isBlank()) return@withContext ApiResult.DeviceError("无法读取原厂登录令牌 LD，请确认设备 Web 服务可用")

        val hashedPassword = CryptoUtils.sha256HexUpper(CryptoUtils.sha256HexUpper(config.adminPassword) + ld)
        val modes = when (config.loginModePreference) {
            LoginModePreference.MultiUserFirst -> listOf("LOGIN_MULTI_USER", "LOGIN")
            LoginModePreference.LegacyFirst -> listOf("LOGIN", "LOGIN_MULTI_USER")
        }

        var lastError = "原厂管理员密码错误或设备拒绝登录"
        var hadSuccessfulLoginWithoutCookie = false
        for (mode in modes) {
            val form = linkedMapOf(
                "goformId" to mode,
                "isTest" to "false",
                "password" to hashedPassword,
                "user" to "admin"
            )
            if (mode == "LOGIN_MULTI_USER") form["IP"] = "localhost"

            when (val response = execute(config, "POST", "/api/goform/goform_set_cmd_process", formBody(form), null)) {
                is RawResponse.Success -> {
                    val body = response.bodyText
                    val result = try {
                        parseJsonObject(body)["result"]?.jsonPrimitive?.contentOrNull
                    } catch (e: Exception) {
                        return@withContext ApiResult.ParseError("原厂登录响应不是有效 JSON：${e.message}")
                    }
                    if (result == null) {
                        lastError = "原厂登录响应缺少 result 字段"
                        continue
                    }
                    if (result.trim() == "3") {
                        lastError = "原厂管理员密码错误或登录被设备拒绝"
                        continue
                    }

                    val cookie = extractCookie(response.headers)
                    if (cookie.isBlank()) {
                        hadSuccessfulLoginWithoutCookie = true
                        continue
                    }
                    return@withContext ApiResult.Success(GoformSession(cookie))
                }
                RawResponse.Unauthorized -> return@withContext ApiResult.Unauthorized
                is RawResponse.NetworkError -> return@withContext ApiResult.NetworkError(response.message)
            }
        }

        if (hadSuccessfulLoginWithoutCookie) {
            ApiResult.DeviceError("原厂登录成功但没有返回 Cookie，无法执行短信写操作")
        } else {
            ApiResult.DeviceError(lastError)
        }
    }

    suspend fun updateAdminPassword(config: ConnectionConfig): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val body = JsonObject(mapOf("password" to JsonPrimitive(config.adminPassword)))
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        when (val result = executeJson(config, "POST", "/api/update_admin_pwd", body, null)) {
            is ApiResult.Success -> {
                val resultText = result.value["result"]?.jsonPrimitive?.contentOrNull
                val error = result.value["error"]?.jsonPrimitive?.contentOrNull
                if (resultText.equals("success", ignoreCase = true) || resultText.equals("ok", ignoreCase = true)) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.DeviceError(error ?: "同步原厂管理员密码失败：result=$resultText")
                }
            }
            is ApiResult.Unauthorized -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ParseError -> result
            is ApiResult.DeviceError -> result
        }
    }

    suspend fun prepareAd(config: ConnectionConfig, session: GoformSession): ApiResult<String> {
        val info = when (val result = getGoformValue(config, "wa_inner_version,cr_version", session.cookie)) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }
        val wa = info["wa_inner_version"].orEmpty()
        val cr = info["cr_version"].orEmpty()
        if (wa.isBlank() || cr.isBlank()) return ApiResult.DeviceError("无法读取原厂版本信息，不能计算 AD")

        val rd = when (val result = getGoformValue(config, "RD", session.cookie)) {
            is ApiResult.Success -> result.value["RD"].orEmpty()
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }
        if (rd.isBlank()) return ApiResult.DeviceError("无法读取原厂随机令牌 RD，不能计算 AD")
        return ApiResult.Success(CryptoUtils.sha256HexUpper(CryptoUtils.sha256HexUpper(wa + cr) + rd))
    }

    suspend fun logout(config: ConnectionConfig, session: GoformSession): ApiResult<Unit> {
        val ad = when (val result = prepareAd(config, session)) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }
        return when (val result = postForm(
            config,
            "/api/goform/goform_set_cmd_process",
            mapOf("goformId" to "LOGOUT", "isTest" to "false", "AD" to ad),
            session.cookie
        )) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Unauthorized -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ParseError -> result
            is ApiResult.DeviceError -> result
        }
    }

    private suspend fun getGoformValue(config: ConnectionConfig, cmd: String, kanoCookie: String?): ApiResult<JsonObject> {
        val multiData = if (cmd.contains(",")) "&multi_data=1" else ""
        val query = "/api/goform/goform_get_cmd_process?isTest=false&cmd=${cmd.urlEncode()}$multiData&_=${clock()}"
        return executeJson(config, "GET", query, null, kanoCookie)
    }

    private suspend fun executeJson(
        config: ConnectionConfig,
        method: String,
        pathWithQuery: String,
        body: RequestBody?,
        kanoCookie: String?
    ): ApiResult<JsonObject> = withContext(Dispatchers.IO) {
        when (val raw = execute(config, method, pathWithQuery, body, kanoCookie)) {
            is RawResponse.Success -> {
                try {
                    ApiResult.Success(parseJsonObject(raw.bodyText))
                } catch (e: Exception) {
                    ApiResult.ParseError("响应不是有效 JSON：${e.message}")
                }
            }
            RawResponse.Unauthorized -> ApiResult.Unauthorized
            is RawResponse.NetworkError -> ApiResult.NetworkError(raw.message)
        }
    }

    private fun execute(
        config: ConnectionConfig,
        method: String,
        pathWithQuery: String,
        body: RequestBody?,
        kanoCookie: String?
    ): RawResponse {
        return try {
            val url = config.normalizedBaseUrl + pathWithQuery
            val pathForSign = pathWithQuery.substringBefore("?")
            val timestamp = clock()
            val tokenHash = CryptoUtils.sha256Hex(config.ufiToken).lowercase()
            val request = Request.Builder()
                .url(url)
                .method(method, body)
                .header("kano-t", timestamp.toString())
                .header("kano-sign", CryptoUtils.kanoSignature(method = method, path = pathForSign, timestampMillis = timestamp))
                .header("Authorization", tokenHash)
                .header("Accept", "application/json")
                .apply {
                    if (!kanoCookie.isNullOrBlank()) {
                        header("Cookie", kanoCookie)
                        header("Kano-Cookie", kanoCookie)
                    }
                }
                .build()

            clientFor(config).newCall(request).execute().use { response ->
                if (response.code == 401) return RawResponse.Unauthorized
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    RawResponse.NetworkError("HTTP ${response.code}: ${text.take(180)}")
                } else {
                    RawResponse.Success(text, response.headers.asMap())
                }
            }
        } catch (e: IOException) {
            RawResponse.NetworkError(config.accessFailureMessage("网络请求失败：${e.message}"))
        } catch (e: IllegalArgumentException) {
            RawResponse.NetworkError(config.accessFailureMessage("连接配置无效：${e.message}"))
        }
    }

    private fun clientFor(config: ConnectionConfig): OkHttpClient {
        return when (config.accessMode) {
            UfiAccessMode.Direct -> httpClient.newBuilder()
                .proxy(Proxy.NO_PROXY)
                .build()
            UfiAccessMode.EasyTierSocks5 -> {
                val host = config.normalizedEasyTierSocks5Host
                require(host.isNotBlank()) { "EasyTier SOCKS5 代理地址不能为空" }
                require(config.easyTierSocks5Port in 1..65535) { "EasyTier SOCKS5 代理端口必须在 1..65535 之间" }
                httpClient.newBuilder()
                    .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, config.easyTierSocks5Port)))
                    .build()
            }
        }
    }

    private fun ConnectionConfig.accessFailureMessage(detail: String): String {
        val mode = when (accessMode) {
            UfiAccessMode.Direct -> "直连"
            UfiAccessMode.EasyTierSocks5 -> "EasyTier SOCKS5"
        }
        return "$mode 访问失败：$detail"
    }

    private fun parseJsonObject(text: String): JsonObject {
        val element: JsonElement = json.parseToJsonElement(text)
        return element.jsonObject
    }

    private fun formBody(form: Map<String, String>): RequestBody {
        val builder = FormBody.Builder()
        form.forEach { (key, value) -> builder.add(key, value) }
        return builder.build()
    }

    private fun extractCookie(headers: Map<String, String>): String {
        return headers["kano-cookie"]
            ?.substringBefore(';')
            ?.takeIf { it.isNotBlank() }
            ?: headers["set-cookie"]?.substringBefore(';').orEmpty()
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
    }

    private fun JsonElement?.orEmpty(): String = this?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun okhttp3.Headers.asMap(): Map<String, String> {
        val values = mutableMapOf<String, String>()
        for (index in 0 until size) {
            values[name(index)] = value(index)
            values[name(index).lowercase()] = value(index)
        }
        return values
    }

    private sealed interface RawResponse {
        data class Success(val bodyText: String, val headers: Map<String, String>) : RawResponse
        data object Unauthorized : RawResponse
        data class NetworkError(val message: String) : RawResponse
    }
}

data class GoformSession(val cookie: String)
