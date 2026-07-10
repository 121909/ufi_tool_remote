package com.example.ufitoolsremote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ufitoolsremote.model.ApiResult
import com.example.ufitoolsremote.model.SmsMessage
import com.example.ufitoolsremote.model.messageOrNull
import com.example.ufitoolsremote.model.resolvedConnectionConfig
import com.example.ufitoolsremote.util.SmsContentUtils
import com.example.ufitoolsremote.widget.WidgetActions
import com.example.ufitoolsremote.widget.WidgetUpdater
import kotlinx.coroutines.launch

class SmsDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UfiTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SmsDetailScreen(
                        messageId = intent.getStringExtra(WidgetActions.EXTRA_MESSAGE_ID).orEmpty(),
                        fallbackNumber = intent.getStringExtra(WidgetActions.EXTRA_NUMBER).orEmpty(),
                        fallbackContent = intent.getStringExtra(WidgetActions.EXTRA_MESSAGE).orEmpty(),
                        fallbackDate = intent.getStringExtra(WidgetActions.EXTRA_DATE).orEmpty(),
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsDetailScreen(
    messageId: String,
    fallbackNumber: String,
    fallbackContent: String,
    fallbackDate: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as UfiRemoteApplication
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var deleting by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(
            app.container.smsCacheRepository.loadMessages().firstOrNull { it.id == messageId }
                ?: SmsMessage(
                    id = messageId,
                    number = fallbackNumber,
                    content = fallbackContent,
                    rawContent = fallbackContent,
                    date = fallbackDate,
                    tag = "",
                    isUnread = false,
                    isFailed = false
                )
        )
    }

    val displayContent = remember(message.content, message.rawContent) {
        message.content.ifBlank { message.rawContent }
    }
    val links = remember(displayContent) { SmsContentUtils.extractLinks(displayContent) }
    val code = remember(displayContent) { SmsContentUtils.extractVerificationCode(displayContent) }
    val formattedDate = remember(message.date) { formatSmsDate(message.date) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = message.number.ifBlank { "短信详情" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { confirmDelete = true },
                            enabled = !deleting && message.id.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (message.tag.isNotBlank()) {
                                    StatusPill(message.tag, active = false)
                                }
                                if (message.isUnread) {
                                    StatusPill("未读", active = true)
                                }
                                if (message.isFailed) {
                                    BadgeText("失败", MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                text = message.number.ifBlank { "未知号码" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formattedDate.ifBlank { "时间未知" },
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (deleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    InfoRow("消息 ID", message.id.ifBlank { "-" }, monospace = true)
                    InfoRow("号码", message.number.ifBlank { "-" })
                    InfoRow("标签", message.tag.ifBlank { "-" })
                    InfoRow("内容长度", displayContent.length.toString())
                }
            }

            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "内容",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "正文可直接复制或选中查看",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        OutlinedButton(
                            enabled = displayContent.isNotBlank(),
                            onClick = {
                                clipboard.setText(AnnotatedString(displayContent))
                                scope.launch { snackbarHostState.showSnackbar("正文已复制") }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("复制正文")
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = displayContent.ifBlank { "暂无内容" },
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )
                    }
                }
            }

            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "提取结果",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (code != null || links.isNotEmpty()) {
                                    "提取到可直接处理的信息"
                                } else {
                                    "未提取到验证码或链接"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            if (code != null) {
                                StatusPill("验证码", active = true)
                            }
                            if (links.isNotEmpty()) {
                                StatusPill("${links.size} 个链接", active = true)
                            }
                        }
                    }

                    if (code != null) {
                        ActionCard(
                            title = "验证码",
                            value = code,
                            icon = Icons.Default.ContentCopy,
                            actionText = "复制",
                            actionIcon = Icons.Default.ContentCopy,
                            onAction = {
                                clipboard.setText(AnnotatedString(code))
                                scope.launch { snackbarHostState.showSnackbar("验证码已复制") }
                            }
                        )
                    }

                    if (links.isNotEmpty()) {
                        links.forEach { link ->
                            ActionCard(
                                title = "链接",
                                value = link,
                                icon = Icons.Default.Link,
                                actionText = "打开",
                                actionIcon = Icons.Default.OpenInBrowser,
                                onAction = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                    }.onFailure {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("无法打开链接")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (code == null && links.isEmpty()) {
                        EmptyPanel("没有可提取的验证码或链接")
                    }
                }
            }

            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "快捷操作",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !deleting && message.number.isNotBlank(),
                            onClick = {
                                context.startActivity(Intent(context, ComposeSmsActivity::class.java).apply {
                                    putExtra(WidgetActions.EXTRA_NUMBER, message.number)
                                    putExtra(WidgetActions.EXTRA_LABEL, "回复 ${message.number}")
                                })
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("回复")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = message.number.isNotBlank(),
                            onClick = {
                                clipboard.setText(AnnotatedString(message.number))
                                scope.launch { snackbarHostState.showSnackbar("号码已复制") }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("复制号码")
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = displayContent.isNotBlank(),
                        onClick = {
                            clipboard.setText(AnnotatedString(displayContent))
                            scope.launch { snackbarHostState.showSnackbar("正文已复制") }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("复制正文")
                    }
                }
            }

            Spacer(Modifier.height(72.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除短信") },
            text = { Text("确定要删除这条短信吗？") },
            confirmButton = {
                Button(
                    enabled = !deleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    onClick = {
                        confirmDelete = false
                        deleting = true
                        scope.launch {
                            val config = app.container.settingsRepository.current().resolvedConnectionConfig()
                            when (val result = app.container.smsRepository.deleteSms(config, message.id)) {
                                is ApiResult.Success -> {
                                    val refreshed = app.container.smsRepository.fetchSms(config)
                                    if (refreshed is ApiResult.Success) {
                                        app.container.smsCacheRepository.save(
                                            refreshed.value,
                                            statusText = "已删除短信",
                                            statusAtMillis = System.currentTimeMillis()
                                        )
                                        message = refreshed.value.firstOrNull { it.id == message.id } ?: message
                                    } else {
                                        app.container.smsCacheRepository.updateStatus("已删除短信")
                                    }
                                    WidgetUpdater.updateAll(context.applicationContext)
                                    snackbarHostState.showSnackbar("短信已删除")
                                    onClose()
                                }
                                else -> snackbarHostState.showSnackbar(result.messageOrNull().orEmpty())
                            }
                            deleting = false
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    value,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (title == "验证码") FontFamily.Monospace else FontFamily.Default
                )
            }
            OutlinedButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(actionText)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (monospace) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun StatusPill(text: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun BadgeText(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "可以复制正文，或返回后继续查看其它短信。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun formatSmsDate(raw: String): String {
    val parts = raw.split(",")
    return if (parts.size >= 5) "${parts[0]}-${parts[1]}-${parts[2]} ${parts[3]}:${parts[4]}" else raw
}
