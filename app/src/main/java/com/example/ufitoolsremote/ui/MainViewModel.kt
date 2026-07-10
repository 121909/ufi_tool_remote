package com.example.ufitoolsremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ufitoolsremote.UfiRemoteApplication
import com.example.ufitoolsremote.easytier.EasyTierConfigBuilder
import com.example.ufitoolsremote.easytier.EasyTierRuntime
import com.example.ufitoolsremote.easytier.EasyTierService
import com.example.ufitoolsremote.easytier.mergeEasyTierStatus
import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.AppSettings
import com.example.ufitoolsremote.model.ConnectionConfig
import com.example.ufitoolsremote.model.DeviceInfo
import com.example.ufitoolsremote.model.EasyTierSettings
import com.example.ufitoolsremote.model.EasyTierStatus
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.QuickReplyPreset
import com.example.ufitoolsremote.model.QuickReplySendMode
import com.example.ufitoolsremote.model.SmsMessage
import com.example.ufitoolsremote.model.UfiAccessMode
import com.example.ufitoolsremote.model.messageOrNull
import com.example.ufitoolsremote.model.resolvedConnectionConfig
import com.example.ufitoolsremote.widget.WidgetScheduler
import com.example.ufitoolsremote.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UfiRemoteApplication
    private val deviceRepository = app.container.deviceRepository
    private val smsRepository = app.container.smsRepository
    private val settingsRepository = app.container.settingsRepository
    private val cacheRepository = app.container.smsCacheRepository

    private val _uiState = MutableStateFlow(
        UfiUiState(
            settings = settingsRepository.current(),
            messages = cacheRepository.loadMessages(),
            easyTierDraft = settingsRepository.current().easyTier,
            easyTierSocks5PortDraft = settingsRepository.current().easyTier.socks5Port.toString()
        )
    )
    val uiState: StateFlow<UfiUiState> = _uiState.asStateFlow()
    private var didStartupRefresh = false
    private var easyTierStatusJob: Job? = null

    init {
        val initialSettings = settingsRepository.current()
        WidgetScheduler.schedule(app, initialSettings.widgetRefreshMinutes)
        if (initialSettings.easyTier.enabled) {
            EasyTierService.start(app)
            startEasyTierStatusLoop()
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                if (settings.easyTier.enabled) {
                    startEasyTierStatusLoop()
                } else {
                    stopEasyTierStatusLoop()
                    _uiState.update { it.copy(easyTierStatus = EasyTierStatus()) }
                }
                maybeStartupRefresh(settings)
            }
        }
    }

    fun updateBaseUrl(value: String) = updateConnection { copy(baseUrl = value) }
    fun updateUfiToken(value: String) = updateConnection { copy(ufiToken = value) }
    fun updateAdminPassword(value: String) = updateConnection { copy(adminPassword = value) }
    fun updateLoginMode(value: LoginModePreference) = updateSettings { copy(connection = connection.copy(loginModePreference = value)) }
    fun updateUfiAccessMode(value: UfiAccessMode) = updateSettings { copy(connection = connection.copy(accessMode = value)) }
    fun updateEasyTierDraft(transform: EasyTierSettings.() -> EasyTierSettings) {
        _uiState.update {
            it.copy(
                easyTierDraft = it.easyTierDraft.transform(),
                easyTierValidationError = null
            )
        }
    }

    fun updateEasyTierSocks5PortDraft(value: String) {
        if (value.any { !it.isDigit() }) return
        _uiState.update {
            it.copy(
                easyTierSocks5PortDraft = value,
                easyTierValidationError = null
            )
        }
    }

    fun discardEasyTierDraft() {
        val persisted = settingsRepository.current().easyTier
        _uiState.update {
            it.copy(
                easyTierDraft = persisted,
                easyTierSocks5PortDraft = persisted.socks5Port.toString(),
                easyTierValidationError = null
            )
        }
    }

    fun applyEasyTierSettings() {
        val previous = settingsRepository.current().easyTier
        val state = _uiState.value
        val port = state.easyTierSocks5PortDraft.toIntOrNull()
        if (state.easyTierDraft.socks5Enabled && (port == null || port !in 1..65535)) {
            _uiState.update { it.copy(easyTierValidationError = EASYTIER_PORT_ERROR) }
            return
        }

        val candidate = state.easyTierDraft.copy(
            enabled = previous.enabled,
            socks5Port = port?.takeIf { it in 1..65535 } ?: previous.socks5Port
        )
        val validation = EasyTierConfigBuilder.validate(candidate)
        if (validation != null) {
            _uiState.update { it.copy(easyTierValidationError = validation) }
            return
        }

        val saved = settingsRepository.updateEasyTier { candidate }
        val configurationChanged = previous != saved.easyTier
        _uiState.update {
            it.copy(
                settings = saved,
                easyTierDraft = saved.easyTier,
                easyTierSocks5PortDraft = saved.easyTier.socks5Port.toString(),
                easyTierValidationError = null,
                message = if (previous.enabled && configurationChanged) {
                    "EasyTier 设置已保存，正在重启"
                } else {
                    "EasyTier 设置已保存"
                }
            )
        }

        if (previous.enabled && configurationChanged) {
            EasyTierService.restart(app)
            refreshEasyTierStatus()
        }
    }

    fun setEasyTierEnabled(enabled: Boolean) {
        if (enabled) {
            if (_uiState.value.hasEasyTierDraftChanges) {
                _uiState.update { it.copy(message = "请先应用 EasyTier 配置修改，再启用服务") }
                return
            }
            val candidate = settingsRepository.current().easyTier.copy(enabled = true)
            val validation = EasyTierConfigBuilder.validate(candidate)
            if (validation != null) {
                _uiState.update { it.copy(message = validation) }
                return
            }
            val updated = settingsRepository.updateEasyTier { copy(enabled = true) }
            _uiState.update { it.copy(settings = updated, message = "EasyTier 正在启动") }
            EasyTierService.start(app)
            startEasyTierStatusLoop()
            refreshEasyTierStatus()
        } else {
            val updated = settingsRepository.updateEasyTier { copy(enabled = false) }
            _uiState.update { it.copy(settings = updated, message = "EasyTier 正在停止") }
            EasyTierService.stop(app)
            stopEasyTierStatusLoop()
            _uiState.update { it.copy(easyTierStatus = EasyTierStatus()) }
        }
    }

    fun refreshEasyTierStatus() {
        viewModelScope.launch {
            loadEasyTierStatus(showLoading = true)
        }
    }

    fun updateWidgetRefreshMinutes(value: Int) {
        val updated = settingsRepository.updateWidgetRefreshMinutes(value)
        _uiState.update { it.copy(settings = updated) }
        WidgetScheduler.schedule(app, updated.widgetRefreshMinutes)
        WidgetUpdater.updateAll(app)
    }

    fun addQuickReply() {
        val updated = settingsRepository.upsertQuickReply(
            QuickReplyPreset(
                label = "快捷回复",
                number = "",
                message = "",
                sendMode = QuickReplySendMode.Confirm
            )
        )
        _uiState.update { it.copy(settings = updated) }
        WidgetUpdater.updateAll(app)
    }

    fun updateQuickReply(preset: QuickReplyPreset) {
        val updated = settingsRepository.upsertQuickReply(preset)
        _uiState.update { it.copy(settings = updated) }
        WidgetUpdater.updateAll(app)
    }

    fun deleteQuickReply(id: String) {
        val updated = settingsRepository.deleteQuickReply(id)
        _uiState.update { it.copy(settings = updated) }
        WidgetUpdater.updateAll(app)
    }

    fun connectAndRefresh() {
        val config = resolvedConnection()
        if (config.normalizedBaseUrl.isBlank() || config.ufiToken.isBlank() || config.adminPassword.isBlank()) {
            _uiState.update { it.copy(message = "请填写地址、UFI-TOOLS 口令和原厂管理员密码") }
            return
        }
        viewModelScope.launch {
            runEasyTierBeforeRefresh(uiState.value.settings, manual = true)
        }
    }

    fun refreshAll() {
        refreshDevice()
        refreshSms()
    }

    fun refreshDevice() {
        val config = resolvedConnection()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDevice = true, message = null) }
            when (val result = deviceRepository.fetchDeviceInfo(config)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(deviceInfo = result.value, isLoadingDevice = false, message = "设备信息已更新")
                }
                else -> _uiState.update { it.copy(isLoadingDevice = false, message = result.messageOrNull()) }
            }
        }
    }

    fun refreshSms() {
        val config = resolvedConnection()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSms = true, message = null) }
            when (val result = smsRepository.fetchSms(config)) {
                is ApiResult.Success -> {
                    val markReadResult = smsRepository.markUnreadMessagesRead(config, result.value)
                    val successfulIds = (markReadResult as? ApiResult.Success)?.value?.successfulIds.orEmpty()
                    val messages = result.value.map { message ->
                        if (message.id.trim() in successfulIds) {
                            message.copy(tag = "0", isUnread = false)
                        } else {
                            message
                        }
                    }
                    val failedCount = when (markReadResult) {
                        is ApiResult.Success -> markReadResult.value.failedIds.size
                        else -> result.value.count { it.isUnread }
                    }
                    val statusText = if (failedCount > 0) {
                        "短信已刷新，$failedCount 条未读状态同步失败"
                    } else {
                        "短信已刷新"
                    }
                    cacheRepository.save(
                        messages,
                        statusText = statusText,
                        statusAtMillis = System.currentTimeMillis()
                    )
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoadingSms = false,
                            message = if (failedCount > 0) {
                                "短信已更新，$failedCount 条未读状态同步失败"
                            } else {
                                "短信已更新"
                            }
                        )
                    }
                    WidgetUpdater.updateAll(app)
                }
                else -> {
                    cacheRepository.updateStatus("刷新失败：${result.messageOrNull().orEmpty()}")
                    WidgetUpdater.updateAll(app)
                    _uiState.update { it.copy(isLoadingSms = false, message = result.messageOrNull()) }
                }
            }
        }
    }

    fun sendSms(number: String, content: String) {
        val config = resolvedConnection()
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingSms = true, message = null) }
            when (val result = smsRepository.sendSms(config, number, content)) {
                is ApiResult.Success -> {
                    cacheRepository.updateStatus("短信已发送")
                    WidgetUpdater.updateAll(app)
                    _uiState.update { it.copy(isSendingSms = false, message = "短信已发送") }
                    refreshSms()
                }
                else -> {
                    val text = result.messageOrNull().orEmpty()
                    cacheRepository.updateStatus("发送失败：$text")
                    WidgetUpdater.updateAll(app)
                    _uiState.update { it.copy(isSendingSms = false, message = text) }
                }
            }
        }
    }

    fun deleteSms(id: String) {
        val config = resolvedConnection()
        viewModelScope.launch {
            _uiState.update { it.copy(message = null) }
            when (val result = smsRepository.deleteSms(config, id)) {
                is ApiResult.Success -> {
                    cacheRepository.updateStatus("已删除短信")
                    WidgetUpdater.updateAll(app)
                    refreshSms()
                }
                else -> {
                    val text = result.messageOrNull().orEmpty()
                    cacheRepository.updateStatus("删除失败：$text")
                    WidgetUpdater.updateAll(app)
                    _uiState.update { it.copy(message = text) }
                }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun maybeStartupRefresh(settings: AppSettings) {
        if (didStartupRefresh || !settings.resolvedConnectionConfig().isReadyForRemoteCalls()) return
        didStartupRefresh = true
        viewModelScope.launch {
            runEasyTierBeforeRefresh(settings, manual = false)
        }
    }

    private fun resolvedConnection(): ConnectionConfig {
        return uiState.value.settings.resolvedConnectionConfig()
    }

    private suspend fun runEasyTierBeforeRefresh(settings: AppSettings, manual: Boolean) {
        if (settings.easyTier.enabled) {
            val validation = EasyTierConfigBuilder.validate(settings.easyTier)
            if (validation != null) {
                _uiState.update { it.copy(message = validation) }
                return
            }
            startEasyTierStatusLoop()
            val easyTierStatus = _uiState.value.easyTierStatus
            val alreadyRunning = easyTierStatus.runningCount > 0
            if (!alreadyRunning) {
                _uiState.update {
                    it.copy(
                        message = if (manual) {
                            "正在启动 EasyTier，随后刷新设备信息"
                        } else {
                            "启动 EasyTier 后自动刷新"
                        },
                        isLoadingEasyTierStatus = true
                    )
                }
                EasyTierService.start(app)
                val ready = waitForEasyTierReady()
                if (!ready) {
                    _uiState.update {
                        it.copy(
                            isLoadingEasyTierStatus = false,
                            message = "EasyTier 尚未就绪，已暂停自动刷新"
                        )
                    }
                    return
                }
            } else if (manual) {
                _uiState.update { it.copy(message = "EasyTier 已在运行，正在刷新设备信息") }
            }
        }
        refreshAll()
    }

    private suspend fun waitForEasyTierReady(): Boolean {
        val deadline = System.currentTimeMillis() + EASYTIER_STARTUP_WAIT_MS
        while (System.currentTimeMillis() <= deadline) {
            val previousStatus = _uiState.value.easyTierStatus
            val status = withContext(Dispatchers.IO) { EasyTierRuntime.collectStatus() }
            val mergedStatus = mergeEasyTierStatus(previousStatus, status)
            _uiState.update {
                it.copy(
                    easyTierStatus = mergedStatus,
                    isLoadingEasyTierStatus = false
                )
            }
            if (mergedStatus.runningCount > 0) return true
            if (!mergedStatus.errorMessage.isNullOrBlank()) return false
            delay(EASYTIER_STARTUP_POLL_MS)
        }
        return false
    }

    private fun startEasyTierStatusLoop() {
        if (easyTierStatusJob?.isActive == true) return
        easyTierStatusJob = viewModelScope.launch {
            while (isActive) {
                loadEasyTierStatus(showLoading = false)
                delay(EASYTIER_STATUS_REFRESH_MS)
            }
        }
    }

    private fun stopEasyTierStatusLoop() {
        easyTierStatusJob?.cancel()
        easyTierStatusJob = null
    }

    private suspend fun loadEasyTierStatus(showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoadingEasyTierStatus = true) }
        }
        val previousStatus = _uiState.value.easyTierStatus
        val status = withContext(Dispatchers.IO) { EasyTierRuntime.collectStatus() }
        val mergedStatus = mergeEasyTierStatus(previousStatus, status)
        _uiState.update {
            it.copy(
                easyTierStatus = mergedStatus,
                isLoadingEasyTierStatus = false
            )
        }
    }

    private fun updateConnection(transform: ConnectionConfig.() -> ConnectionConfig) {
        val updated = settingsRepository.updateConnection(transform)
        _uiState.update { it.copy(settings = updated) }
        WidgetUpdater.updateAll(app)
    }

    private fun updateSettings(transform: AppSettings.() -> AppSettings) {
        val updated = settingsRepository.update(transform)
        _uiState.update { it.copy(settings = updated) }
        WidgetUpdater.updateAll(app)
    }

    private fun ConnectionConfig.isReadyForRemoteCalls(): Boolean {
        return normalizedBaseUrl.isNotBlank() && ufiToken.isNotBlank() && adminPassword.isNotBlank()
    }

    companion object {
        private const val EASYTIER_PORT_ERROR = "SOCKS5 port must be between 1 and 65535"
        private const val EASYTIER_STATUS_REFRESH_MS = 5_000L
        private const val EASYTIER_STARTUP_POLL_MS = 1_000L
        private const val EASYTIER_STARTUP_WAIT_MS = 25_000L
    }
}

data class UfiUiState(
    val settings: AppSettings = AppSettings(),
    val deviceInfo: DeviceInfo? = null,
    val messages: List<SmsMessage> = emptyList(),
    val easyTierStatus: EasyTierStatus = EasyTierStatus(),
    val easyTierDraft: EasyTierSettings = EasyTierSettings(),
    val easyTierSocks5PortDraft: String = EasyTierSettings().socks5Port.toString(),
    val easyTierValidationError: String? = null,
    val isLoadingDevice: Boolean = false,
    val isLoadingSms: Boolean = false,
    val isLoadingEasyTierStatus: Boolean = false,
    val isSendingSms: Boolean = false,
    val message: String? = null
) {
    val hasEasyTierDraftChanges: Boolean
        get() {
            val persisted = settings.easyTier
            val comparableDraft = easyTierDraft.copy(
                enabled = persisted.enabled,
                socks5Port = persisted.socks5Port
            )
            return comparableDraft != persisted ||
                easyTierSocks5PortDraft != persisted.socks5Port.toString()
        }
}
