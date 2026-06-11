package com.example.ufitoolsremote.data

import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.SmsMessage
import com.example.ufitoolsremote.network.GoformSession
import com.example.ufitoolsremote.network.SmsCodec
import com.example.ufitoolsremote.network.UfiApiClient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SmsRepository(private val api: UfiApiClient) {
    suspend fun fetchSms(config: ConnectionConfig, page: Int = 0, pageSize: Int = 500): ApiResult<List<SmsMessage>> {
        val path = "/api/goform/goform_get_cmd_process?multi_data=1&isTest=false&cmd=sms_data_total&page=$page&data_per_page=$pageSize&mem_store=1&tags=100&order_by=order%20by%20id%20desc&_=${System.currentTimeMillis()}"
        val json = when (val result = api.getJson(config, path)) {
            is ApiResult.Success -> result.value
            is ApiResult.Unauthorized -> return result
            is ApiResult.NetworkError -> return result
            is ApiResult.ParseError -> return result
            is ApiResult.DeviceError -> return result
        }
        val messages = json["messages"]?.jsonArray ?: JsonArray(emptyList())
        return ApiResult.Success(messages.mapNotNull { it.toSmsMessage() })
    }

    suspend fun sendSms(config: ConnectionConfig, number: String, content: String): ApiResult<Unit> {
        if (number.isBlank()) return ApiResult.DeviceError("请输入手机号")
        if (content.isBlank()) return ApiResult.DeviceError("请输入短信内容")
        return withSession(config) { session ->
            postGoformAction(
                config = config,
                session = session,
                form = linkedMapOf(
                    "goformId" to "SEND_SMS",
                    "Number" to number,
                    "MessageBody" to SmsCodec.encodeUtf16BeHex(content)
                )
            )
        }
    }

    suspend fun deleteSms(config: ConnectionConfig, id: String): ApiResult<Unit> {
        val trimmed = id.trim()
        if (trimmed.isBlank()) return ApiResult.DeviceError("缺少短信 ID")
        return withSession(config) { session ->
            val first = deleteSmsWithId(config, session, trimmed)
            if (first is ApiResult.DeviceError && !trimmed.endsWith(";")) {
                deleteSmsWithId(config, session, "$trimmed;")
            } else {
                first
            }
        }
    }

    suspend fun markRead(config: ConnectionConfig, id: String): ApiResult<Unit> {
        val trimmed = id.trim()
        if (trimmed.isBlank()) return ApiResult.DeviceError("缺少短信 ID")
        return withSession(config) { session ->
            val first = markReadWithId(config, session, trimmed)
            if (first is ApiResult.DeviceError && !trimmed.endsWith(";")) {
                markReadWithId(config, session, "$trimmed;")
            } else {
                first
            }
        }
    }

    suspend fun markUnreadMessagesRead(config: ConnectionConfig, messages: List<SmsMessage>) {
        messages.filter { it.isUnread }.forEach { markRead(config, it.id) }
    }

    private suspend fun deleteSmsWithId(
        config: ConnectionConfig,
        session: GoformSession,
        id: String
    ): ApiResult<Unit> {
        return postGoformAction(
            config = config,
            session = session,
            form = linkedMapOf(
                "goformId" to "DELETE_SMS",
                "msg_id" to id,
                "notCallback" to "true"
            )
        )
    }

    private suspend fun markReadWithId(
        config: ConnectionConfig,
        session: GoformSession,
        id: String
    ): ApiResult<Unit> {
        return postGoformAction(
            config = config,
            session = session,
            form = linkedMapOf(
                "goformId" to "SET_MSG_READ",
                "msg_id" to id,
                "notCallback" to "true"
            )
        )
    }

    private suspend fun postGoformAction(
        config: ConnectionConfig,
        session: GoformSession,
        form: LinkedHashMap<String, String>
    ): ApiResult<Unit> {
        val ad = when (val adResult = api.prepareAd(config, session)) {
            is ApiResult.Success -> adResult.value
            is ApiResult.Unauthorized -> return adResult
            is ApiResult.NetworkError -> return adResult
            is ApiResult.ParseError -> return adResult
            is ApiResult.DeviceError -> return adResult
        }
        form["isTest"] = "false"
        form["AD"] = ad
        return api.postForm(config, "/api/goform/goform_set_cmd_process", form, session.cookie).unitFromGoform()
    }

    private suspend fun withSession(
        config: ConnectionConfig,
        block: suspend (GoformSession) -> ApiResult<Unit>
    ): ApiResult<Unit> {
        val session = when (val login = api.loginGoform(config)) {
            is ApiResult.Success -> login.value
            is ApiResult.Unauthorized -> return login
            is ApiResult.NetworkError -> return login
            is ApiResult.ParseError -> return login
            is ApiResult.DeviceError -> return login
        }
        api.updateAdminPassword(config)
        return try {
            block(session)
        } finally {
            api.logout(config, session)
        }
    }

    private fun ApiResult<JsonObject>.unitFromGoform(): ApiResult<Unit> = when (this) {
        is ApiResult.Success -> {
            val result = value["result"]?.jsonPrimitive?.contentOrNull
            val message = value["message"]?.jsonPrimitive?.contentOrNull
            val error = value["error"]?.jsonPrimitive?.contentOrNull
            if (result.isSuccessfulGoformResult()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.DeviceError(message ?: error ?: "设备动作失败：result=$result")
            }
        }
        is ApiResult.Unauthorized -> this
        is ApiResult.NetworkError -> this
        is ApiResult.ParseError -> this
        is ApiResult.DeviceError -> this
    }

    private fun String?.isSuccessfulGoformResult(): Boolean {
        val normalized = this?.trim().orEmpty()
        return normalized.equals("success", ignoreCase = true) ||
            normalized.equals("ok", ignoreCase = true) ||
            normalized == "0" ||
            normalized == "1"
    }

    private fun JsonElement.toSmsMessage(): SmsMessage? {
        val obj = runCatching { jsonObject }.getOrNull() ?: return null
        val id = obj.string("id") ?: return null
        val raw = obj.string("content").orEmpty()
        val decoded = runCatching { SmsCodec.decodeBase64Utf8(raw) }.getOrDefault(raw)
        val tag = obj.string("tag").orEmpty()
        return SmsMessage(
            id = id,
            number = obj.string("number").orEmpty(),
            content = decoded,
            rawContent = raw,
            date = obj.string("date").orEmpty(),
            tag = tag,
            isUnread = tag == "1",
            isFailed = tag == "3"
        )
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
