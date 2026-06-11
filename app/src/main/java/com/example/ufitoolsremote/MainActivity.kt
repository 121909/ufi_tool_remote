package com.example.ufitoolsremote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ufitoolsremote.model.DeviceInfo
import com.example.ufitoolsremote.model.EasyTierPeerConnectionStatus
import com.example.ufitoolsremote.model.EasyTierPeerStatus
import com.example.ufitoolsremote.model.EasyTierSettings
import com.example.ufitoolsremote.model.EasyTierStatus
import com.example.ufitoolsremote.model.LoginModePreference
import com.example.ufitoolsremote.model.QuickReplyPreset
import com.example.ufitoolsremote.model.QuickReplySendMode
import com.example.ufitoolsremote.model.SmsMessage
import com.example.ufitoolsremote.model.StorageInfo
import com.example.ufitoolsremote.ui.MainViewModel
import com.example.ufitoolsremote.widget.WidgetActions
import com.example.ufitoolsremote.widget.WidgetUpdater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UfiTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    UfiRemoteApp()
                }
            }
        }
    }
}

private val SmsFilterSegmentLabels = listOf("全部", "未读", "失败")
private val QuickReplySendModeSegmentLabels = listOf("直接发送", "确认发送")
private val LoginModeSegmentLabels = listOf("多用户优先", "兼容优先")
private val CompactSegmentedButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSegmentedButtonRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            SegmentedButton(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                selected = selected,
                onClick = { onSelectedIndexChange(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                contentPadding = CompactSegmentedButtonPadding,
                icon = {}
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun UfiRemoteApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var sendDraft by remember { mutableStateOf<SendDraft?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HeaderBar(
                title = when (selectedTab) {
                    0 -> "设备监控"
                    1 -> "短信收件箱"
                    else -> "连接与小组件"
                },
                loading = state.isLoadingDevice || state.isLoadingSms || state.isLoadingEasyTierStatus,
                onRefresh = {
                    when (selectedTab) {
                        0 -> viewModel.connectAndRefresh()
                        1 -> viewModel.refreshSms()
                        else -> WidgetUpdater.updateAll(context.applicationContext)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf("设备", "短信", "设置").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Router
                                    1 -> Icons.AutoMirrored.Filled.Send
                                    else -> Icons.Default.Storage
                                },
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { sendDraft = SendDraft() },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    text = { Text("发送") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> DeviceTab(
                modifier = Modifier.padding(padding),
                info = state.deviceInfo,
                loading = state.isLoadingDevice,
                easyTierSettings = state.settings.easyTier,
                easyTierStatus = state.easyTierStatus,
                loadingEasyTierStatus = state.isLoadingEasyTierStatus,
                onConnectAndRefresh = viewModel::connectAndRefresh,
                onRefreshEasyTier = viewModel::refreshEasyTierStatus
            )
            1 -> SmsTabEnhanced(
                modifier = Modifier.padding(padding),
                messages = state.messages,
                loading = state.isLoadingSms,
                onRefresh = viewModel::refreshSms,
                onReply = { number -> sendDraft = SendDraft(number = number) },
                onDelete = viewModel::deleteSms,
                onOpen = { message ->
                    context.startActivity(Intent(context, SmsDetailActivity::class.java).apply {
                        putExtra(WidgetActions.EXTRA_MESSAGE_ID, message.id)
                        putExtra(WidgetActions.EXTRA_NUMBER, message.number)
                        putExtra(WidgetActions.EXTRA_MESSAGE, message.content)
                        putExtra(WidgetActions.EXTRA_DATE, message.date)
                    })
                }
            )
            2 -> SettingsTab(
                modifier = Modifier.padding(padding),
                state = state,
                easyTierStatus = state.easyTierStatus,
                onBaseUrlChange = viewModel::updateBaseUrl,
                onUfiTokenChange = viewModel::updateUfiToken,
                onAdminPasswordChange = viewModel::updateAdminPassword,
                onLoginModeChange = viewModel::updateLoginMode,
                onEasyTierEnabledChange = viewModel::setEasyTierEnabled,
                onEasyTierChange = viewModel::updateEasyTier,
                onWidgetRefreshMinutesChange = viewModel::updateWidgetRefreshMinutes,
                onAddQuickReply = viewModel::addQuickReply,
                onQuickReplyChange = viewModel::updateQuickReply,
                onQuickReplyDelete = viewModel::deleteQuickReply
            )
        }
    }

    sendDraft?.let { draft ->
        SendSmsDialog(
            sending = state.isSendingSms,
            initialNumber = draft.number,
            initialContent = draft.content,
            onDismiss = { sendDraft = null },
            onSend = { number, content ->
                viewModel.sendSms(number, content)
                sendDraft = null
            }
        )
    }
}

@Composable
private fun HeaderBar(title: String, loading: Boolean, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "UFI Remote",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(10.dp))
            }
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
            ) {
                Text(
                    text = if (loading) "同步中" else "就绪",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
            ) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun DeviceTab(
    modifier: Modifier,
    info: DeviceInfo?,
    loading: Boolean,
    easyTierSettings: EasyTierSettings,
    easyTierStatus: EasyTierStatus,
    loadingEasyTierStatus: Boolean,
    onConnectAndRefresh: () -> Unit,
    onRefreshEasyTier: () -> Unit
) {
    var selectedPeer by remember { mutableStateOf<EasyTierPeerStatus?>(null) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DeviceOverviewCard(
                info = info,
                loading = loading,
                easyTierSettings = easyTierSettings,
                easyTierStatus = easyTierStatus,
                onConnectAndRefresh = onConnectAndRefresh
            )
        }
        item {
            EasyTierStatusPanel(
                settings = easyTierSettings,
                status = easyTierStatus,
                loading = loadingEasyTierStatus,
                onRefresh = onRefreshEasyTier,
                onPeerClick = { selectedPeer = it }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricTile("电量", info?.battery, Icons.Default.BatteryFull, Modifier.weight(1f))
                    MetricTile("信号", info?.signal ?: info?.networkType, Icons.Default.SignalCellularAlt, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricTile("温度", info?.cpuTemp, Icons.Default.Thermostat, Modifier.weight(1f))
                    MetricTile("内存", info?.memUsage, Icons.Default.Memory, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricTile("已用流量", usedTrafficSummary(info), Icons.Default.Storage, Modifier.weight(1f))
                    MetricTile("接入设备", info?.connectedDevices, Icons.Default.Router, Modifier.weight(1f))
                }
            }
        }
        item {
            InfoPanel(
                title = "网络",
                rows = listOf(
                    "网络类型" to info?.networkType,
                    "运营商" to info?.provider,
                    "IPv4" to info?.ipv4,
                    "IPv6" to info?.ipv6,
                    "LAN IP" to info?.lanIp,
                    "MAC" to info?.mac,
                    "接入设备" to info?.connectedDevices,
                    "RX" to info?.rxRate,
                    "TX" to info?.txRate
                )
            )
        }
        item {
            InfoPanel(
                title = "流量",
                rows = listOf(
                    "当日流量" to info?.dailyData?.formatBytes(),
                    "本月流量" to info?.monthlyData?.formatBytes(),
                    "官方月流量" to info?.monthlyTotalBytes?.formatBytes(),
                    "月接收" to info?.monthlyRxBytes?.formatBytes(),
                    "月发送" to info?.monthlyTxBytes?.formatBytes()
                )
            )
        }
        item {
            InfoPanel(
                title = "频段与基站",
                rows = listOf(
                    "4G 频段" to info?.lteBand,
                    "4G 频宽" to info?.lteBandwidth,
                    "4G 基站" to info?.lteCellId,
                    "4G PCI" to info?.ltePci,
                    "4G 频点" to info?.lteFrequency,
                    "5G 频段" to info?.nrBand,
                    "5G 频宽" to info?.nrBandwidth,
                    "5G 基站" to info?.nrCellId,
                    "5G PCI" to info?.nrPci,
                    "5G 频点" to info?.nrFrequency
                )
            )
        }
        item {
            InfoPanel(
                title = "身份",
                rows = listOf(
                    "IMEI" to info?.imei,
                    "IMSI" to info?.imsi,
                    "ICCID" to info?.iccid,
                    "App 版本" to info?.appVersion
                )
            )
        }
        item {
            InfoPanel(
                title = "存储",
                rows = storageRows("内部存储", info?.internalStorage) + storageRows("外部存储", info?.externalStorage)
            )
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    selectedPeer?.let { peer ->
        EasyTierPeerDetailDialog(
            peer = peer,
            onDismiss = { selectedPeer = null }
        )
    }
}

@Composable
private fun DeviceOverviewCard(
    info: DeviceInfo?,
    loading: Boolean,
    easyTierSettings: EasyTierSettings,
    easyTierStatus: EasyTierStatus,
    onConnectAndRefresh: () -> Unit
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(active = info != null)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(if (info != null) "已连接" else "未连接", active = info != null)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        info?.model ?: "UFI 设备",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        info?.provider ?: info?.clientIp ?: "等待设备信息",
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactStat("电量", info?.battery ?: "-", Modifier.weight(1f))
                CompactStat("信号", info?.signal ?: info?.networkType ?: "-", Modifier.weight(1f))
                CompactStat(
                    "EasyTier",
                    when {
                        easyTierStatus.runningCount > 0 -> "运行"
                        easyTierSettings.enabled -> "已启用"
                        else -> "关闭"
                    },
                    Modifier.weight(1f)
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = onConnectAndRefresh
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (info == null) "连接并刷新" else "刷新设备状态")
            }
        }
    }
}

@Composable
private fun EasyTierStatusPanel(
    settings: EasyTierSettings,
    status: EasyTierStatus,
    loading: Boolean,
    onRefresh: () -> Unit,
    onPeerClick: (EasyTierPeerStatus) -> Unit
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active = status.runningCount > 0)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("EasyTier 后台网络", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            status.runningCount > 0 -> "运行中 · ${formatClock(status.updatedAtMillis)}"
                            status.errorMessage != null -> status.errorMessage
                            settings.enabled -> if (status.hasInstances) {
                                "实例未运行 · ${formatClock(status.updatedAtMillis)}"
                            } else {
                                "已启用 · 等待启动"
                            }
                            else -> "未启用"
                        },
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新 EasyTier 状态")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactStat("实例", "${status.runningCount}/${status.instanceCount}", Modifier.weight(1f))
                CompactStat("对端", status.peerCount.toString(), Modifier.weight(1f))
                CompactStat("路由", status.routeCount.toString(), Modifier.weight(1f))
            }

            Text(
                if (settings.socks5Enabled) {
                    "SOCKS5 ${settings.socks5Host}:${settings.socks5Port}"
                } else {
                    "SOCKS5 未启用"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            val peers = status.peers.take(5)
            if (status.runningCount > 0 && peers.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                peers.forEach { peer ->
                    val relayLabel = peer.relayLabel()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPeerClick(peer) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDot(active = peer.online || relayLabel != null)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                peer.displayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                listOfNotNull(
                                    peer.virtualIp,
                                    peer.latency,
                                    relayLabel ?: "${peer.connectionCount} 条连接"
                                )
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (settings.enabled) {
                Text("暂无已连接对端", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun EasyTierPeerDetailDialog(peer: EasyTierPeerStatus, onDismiss: () -> Unit) {
    val relayLabel = peer.relayLabel()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(peer.displayName(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                peer.displaySubtitle()?.let { subtitle ->
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("状态", when {
                    peer.online -> "在线"
                    relayLabel != null -> relayLabel
                    else -> "离线"
                })
                DetailRow("虚拟 IP", peer.virtualIp)
                DetailRow("版本", peer.version)
                DetailRow("下一跳", peer.nextHopDisplayText())
                DetailRow("Cost", peer.cost)
                DetailRow("路径延迟", peer.pathLatency ?: peer.latency)
                DetailRow("代理网段", peer.proxyCidrs.takeIf { it.isNotEmpty() }?.joinToString("\n"))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("连接详情", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (peer.connections.isEmpty()) {
                    Text("暂无连接详情", color = MaterialTheme.colorScheme.secondary)
                } else {
                    peer.connections.forEachIndexed { index, connection ->
                        EasyTierConnectionDetail(index + 1, connection)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun EasyTierConnectionDetail(index: Int, connection: EasyTierPeerConnectionStatus) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active = !connection.isClosed)
                Spacer(Modifier.width(8.dp))
                Text("连接 $index", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(if (connection.isClosed) "已关闭" else "活动", color = MaterialTheme.colorScheme.secondary)
            }
            DetailRow("Conn ID", connection.connId)
            DetailRow("Tunnel", connection.tunnelType)
            DetailRow("Local", connection.localAddress)
            DetailRow("Remote", connection.remoteAddress)
            DetailRow("Resolved", connection.resolvedRemoteAddress)
            DetailRow("网络名", connection.networkName)
            DetailRow("延迟", connection.latency)
            DetailRow("丢包", connection.lossRate)
            DetailRow("流量", listOfNotNull(connection.rxBytes?.formatBytes()?.let { "RX $it" }, connection.txBytes?.formatBytes()?.let { "TX $it" }).joinToString(" · "))
            DetailRow("包数", listOfNotNull(connection.rxPackets?.let { "RX $it" }, connection.txPackets?.let { "TX $it" }).joinToString(" · "))
            DetailRow("角色", connection.isClient?.let { if (it) "Client" else "Server" })
            DetailRow("认证", connection.secureAuthLevel)
            DetailRow("身份", connection.peerIdentityType)
            DetailRow("Features", connection.features.takeIf { it.isNotEmpty() }?.joinToString(", "))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(84.dp), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SmsTab(
    modifier: Modifier,
    messages: List<SmsMessage>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onReply: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (SmsMessage) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DashboardCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${messages.size} 条短信", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("未读 ${messages.count { it.isUnread }} · 失败 ${messages.count { it.isFailed }}", color = MaterialTheme.colorScheme.secondary)
                    }
                    OutlinedButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("刷新")
                    }
                    if (loading) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
        if (messages.isEmpty()) {
            item {
                EmptyPanel("暂无短信")
            }
        } else {
            items(messages, key = { it.id }) { message ->
                SmsCard(
                    message = message,
                    onOpen = { onOpen(message) },
                    onReply = { onReply(message.number) },
                    onDelete = { onDelete(message.id) }
                )
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTab(
    modifier: Modifier,
    state: com.example.ufitoolsremote.ui.UfiUiState,
    easyTierStatus: EasyTierStatus,
    onBaseUrlChange: (String) -> Unit,
    onUfiTokenChange: (String) -> Unit,
    onAdminPasswordChange: (String) -> Unit,
    onLoginModeChange: (LoginModePreference) -> Unit,
    onEasyTierEnabledChange: (Boolean) -> Unit,
    onEasyTierChange: (EasyTierSettings.() -> EasyTierSettings) -> Unit,
    onWidgetRefreshMinutesChange: (Int) -> Unit,
    onAddQuickReply: () -> Unit,
    onQuickReplyChange: (QuickReplyPreset) -> Unit,
    onQuickReplyDelete: (String) -> Unit
) {
    var widgetRefreshText by rememberSaveable(state.settings.widgetRefreshMinutes) {
        mutableStateOf(state.settings.widgetRefreshMinutes.toString())
    }

    LaunchedEffect(state.settings.widgetRefreshMinutes) {
        widgetRefreshText = state.settings.widgetRefreshMinutes.toString()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("连接")
            DashboardCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.settings.connection.baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text("Base URL") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.settings.connection.ufiToken,
                        onValueChange = onUfiTokenChange,
                        label = { Text("UFI-TOOLS 口令") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.settings.connection.adminPassword,
                        onValueChange = onAdminPasswordChange,
                        label = { Text("原厂管理员密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text("登录模式", style = MaterialTheme.typography.labelLarge)
                    CompactSegmentedButtonRow(
                        labels = LoginModeSegmentLabels,
                        selectedIndex = if (
                            state.settings.connection.loginModePreference == LoginModePreference.MultiUserFirst
                        ) {
                            0
                        } else {
                            1
                        },
                        onSelectedIndexChange = { index ->
                            onLoginModeChange(
                                if (index == 0) {
                                    LoginModePreference.MultiUserFirst
                                } else {
                                    LoginModePreference.LegacyFirst
                                }
                            )
                        }
                    )
                }
            }
        }

        item {
            SectionHeader("小组件")
            DashboardCard {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = widgetRefreshText,
                    onValueChange = {
                        widgetRefreshText = it
                        it.toIntOrNull()?.let(onWidgetRefreshMinutesChange)
                    },
                    label = { Text("自动刷新间隔（分钟）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        item {
            SectionHeader("EasyTier")
            EasyTierSettingsCard(
                settings = state.settings.easyTier,
                status = state.easyTierStatus,
                onEnabledChange = onEasyTierEnabledChange,
                onChange = onEasyTierChange
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("快捷回复", Modifier.weight(1f))
                OutlinedButton(onClick = onAddQuickReply) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("添加")
                }
            }
        }

        items(state.settings.quickReplies, key = { it.id }) { preset ->
            QuickReplyEditorRow(
                preset = preset,
                onChange = onQuickReplyChange,
                onDelete = { onQuickReplyDelete(preset.id) }
            )
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EasyTierSettingsCard(
    settings: EasyTierSettings,
    status: EasyTierStatus,
    onEnabledChange: (Boolean) -> Unit,
    onChange: (EasyTierSettings.() -> EasyTierSettings) -> Unit
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("EasyTier 核心", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            status.runningCount > 0 -> "运行中：无 TUN，SOCKS5 ${settings.socks5Host}:${settings.socks5Port}"
                            settings.enabled -> if (status.hasInstances) {
                                "已启用：等待 EasyTier 启动"
                            } else {
                                "已启用：无 TUN，SOCKS5 ${settings.socks5Host}:${settings.socks5Port}"
                            }
                            else -> "未启用"
                        },
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settings.networkName,
                onValueChange = { value -> onChange { copy(networkName = value) } },
                label = { Text("网络名") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settings.networkSecret,
                onValueChange = { value -> onChange { copy(networkSecret = value) } },
                label = { Text("网络密钥") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = settings.instanceName,
                    onValueChange = { value -> onChange { copy(instanceName = value) } },
                    label = { Text("实例名") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = settings.hostname,
                    onValueChange = { value -> onChange { copy(hostname = value) } },
                    label = { Text("主机名") },
                    singleLine = true
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settings.virtualIpv4,
                onValueChange = { value -> onChange { copy(virtualIpv4 = value) } },
                label = { Text("虚拟 IPv4（可选）") },
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SOCKS5 代理", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = settings.socks5Enabled,
                    onCheckedChange = { enabled -> onChange { copy(socks5Enabled = enabled) } }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = settings.socks5Host,
                    onValueChange = { value -> onChange { copy(socks5Host = value) } },
                    label = { Text("绑定地址") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.width(120.dp),
                    value = settings.socks5Port.toString(),
                    onValueChange = { value -> value.toIntOrNull()?.let { port -> onChange { copy(socks5Port = port) } } },
                    label = { Text("端口") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settings.peers.joinToString("\n"),
                onValueChange = { value -> onChange { copy(peers = value.toLines()) } },
                label = { Text("对端地址，每行一个") },
                minLines = 2
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settings.listeners.joinToString("\n"),
                onValueChange = { value -> onChange { copy(listeners = value.toLines()) } },
                label = { Text("监听地址，每行一个") },
                minLines = 2
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settings.proxyNetworks.joinToString("\n"),
                onValueChange = { value -> onChange { copy(proxyNetworks = value.toLines()) } },
                label = { Text("代理网段，每行一个 CIDR") },
                minLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickReplyEditorRow(
    preset: QuickReplyPreset,
    onChange: (QuickReplyPreset) -> Unit,
    onDelete: () -> Unit
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("快捷回复", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = preset.label,
                onValueChange = { onChange(preset.copy(label = it)) },
                label = { Text("名称") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = preset.number,
                onValueChange = { onChange(preset.copy(number = it)) },
                label = { Text("号码") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = preset.message,
                onValueChange = { onChange(preset.copy(message = it)) },
                label = { Text("内容") },
                minLines = 2
            )
            CompactSegmentedButtonRow(
                labels = QuickReplySendModeSegmentLabels,
                selectedIndex = if (preset.sendMode == QuickReplySendMode.Direct) 0 else 1,
                onSelectedIndexChange = { index ->
                    onChange(
                        preset.copy(
                            sendMode = if (index == 0) QuickReplySendMode.Direct else QuickReplySendMode.Confirm
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SmsCard(
    message: SmsMessage,
    onOpen: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.number.ifBlank { "未知号码" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(formatSmsDate(message.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                if (message.isUnread) BadgeText("未读", MaterialTheme.colorScheme.primary)
                if (message.isFailed) BadgeText("失败", MaterialTheme.colorScheme.error)
            }
            Text(message.content, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onReply) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("回复")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String?, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Text(value ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InfoPanel(title: String, rows: List<Pair<String, String?>>) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.width(92.dp), color = MaterialTheme.colorScheme.secondary)
                    Text(value ?: "-", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxWidth().padding(14.dp)) {
            content()
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyPanel(text: String) {
    DashboardCard {
        Text(text, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xFF22C55E) else Color(0xFF94A3B8))
    )
}

@Composable
private fun StatusPill(text: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BadgeText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun SendSmsDialog(
    sending: Boolean,
    initialNumber: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var number by rememberSaveable(initialNumber) { mutableStateOf(initialNumber) }
    var content by rememberSaveable(initialContent) { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发送短信") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("手机号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(enabled = !sending, onClick = { onSend(number.trim(), content.trim()) }) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("发送")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SmsTabEnhanced(
    modifier: Modifier,
    messages: List<SmsMessage>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onReply: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (SmsMessage) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterMode by rememberSaveable { mutableIntStateOf(0) }

    val normalizedQuery = query.trim()
    val unreadCount = messages.count { it.isUnread }
    val failedCount = messages.count { it.isFailed }
    val filteredMessages = messages.filter { message ->
        val matchesQuery = normalizedQuery.isBlank() ||
            message.number.contains(normalizedQuery, ignoreCase = true) ||
            message.content.contains(normalizedQuery, ignoreCase = true) ||
            message.date.contains(normalizedQuery, ignoreCase = true) ||
            message.tag.contains(normalizedQuery, ignoreCase = true)
        val matchesFilter = when (filterMode) {
            1 -> message.isUnread
            2 -> message.isFailed
            else -> true
        }
        matchesQuery && matchesFilter
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DashboardCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${filteredMessages.size} / ${messages.size} 条短信",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "未读 $unreadCount · 失败 $failedCount",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        OutlinedButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("刷新")
                        }
                        if (loading) {
                            Spacer(Modifier.width(10.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("搜索号码、内容或日期") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                                }
                            }
                        }
                    )
                    CompactSegmentedButtonRow(
                        labels = SmsFilterSegmentLabels,
                        selectedIndex = filterMode,
                        onSelectedIndexChange = { filterMode = it }
                    )
                }
            }
        }
        if (filteredMessages.isEmpty()) {
            item {
                EmptyPanel(if (messages.isEmpty()) "暂无短信" else "没有匹配的短信")
            }
        } else {
            items(filteredMessages, key = { it.id }) { message ->
                SmsCard(
                    message = message,
                    onOpen = { onOpen(message) },
                    onReply = { onReply(message.number) },
                    onDelete = { onDelete(message.id) }
                )
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

data class SendDraft(
    val number: String = "",
    val content: String = ""
)

@Composable
fun UfiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) UfiDarkColorScheme else UfiLightColorScheme,
        content = content
    )
}

val UfiColorScheme
    get() = UfiLightColorScheme

private val UfiLightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF475569),
    tertiary = Color(0xFF2563EB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    background = Color(0xFFEFF4F8),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    outlineVariant = Color(0xFFD7DEE8),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF0F172A)
)

private val UfiDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF052E2B),
    secondary = Color(0xFFCBD5E1),
    tertiary = Color(0xFF60A5FA),
    surface = Color(0xFF101826),
    surfaceVariant = Color(0xFF172133),
    background = Color(0xFF07111F),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF4A1820),
    outlineVariant = Color(0xFF2C3A4F),
    onSurface = Color(0xFFE5EDF7),
    onSurfaceVariant = Color(0xFFE5EDF7)
)

private fun storageRows(prefix: String, storage: StorageInfo?): List<Pair<String, String?>> {
    return listOf(
        "$prefix 已用" to storage?.usedBytes?.formatBytes(),
        "$prefix 可用" to storage?.availableBytes?.formatBytes(),
        "$prefix 总量" to storage?.totalBytes?.formatBytes()
    )
}

private fun usedTrafficSummary(info: DeviceInfo?): String? {
    return info?.monthlyData?.formatBytes()
        ?: info?.monthlyTotalBytes?.formatBytes()
        ?: info?.dailyData?.formatBytes()
}

private fun formatSmsDate(raw: String): String {
    val parts = raw.split(",")
    return if (parts.size >= 5) "${parts[0]}-${parts[1]}-${parts[2]} ${parts[3]}:${parts[4]}" else raw
}

private fun formatClock(millis: Long): String {
    if (millis <= 0L) return "未刷新"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
}

private fun EasyTierPeerStatus.displayName(): String {
    return listOfNotNull(hostname, virtualIp)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?: "未知对端"
}

private fun EasyTierPeerStatus.displaySubtitle(): String? {
    val title = displayName()
    return listOfNotNull(virtualIp, version)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() && it != title }
}

private fun EasyTierPeerStatus.relayLabel(): String? {
    return nextHopReadableParts().firstOrNull()?.let { "经由 $it relay" }
}

private fun EasyTierPeerStatus.nextHopDisplayText(): String? {
    return nextHopReadableParts()
        .joinToString(" / ")
        .takeIf { it.isNotBlank() }
}

private fun EasyTierPeerStatus.nextHopReadableParts(): List<String> {
    val nextHopId = nextHopPeerId?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
    if (nextHopId == peerId.trim()) return emptyList()
    return listOfNotNull(nextHopHostname, nextHopInstanceName, nextHopVirtualIp)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun String.toLines(): List<String> {
    return lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
}

private fun Long.formatBytes(): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}
