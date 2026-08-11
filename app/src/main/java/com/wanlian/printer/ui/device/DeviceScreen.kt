package com.wanlian.printer.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.BluetoothTransport
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.PrinterDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (PrinterDevice) -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打印设备") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CurrentDeviceCard(state, onDisconnect, onReconnect)

            state.lastDevice?.let { last ->
                if (state.currentDevice?.address != last.address) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("上次使用：${last.displayName}", fontWeight = FontWeight.SemiBold)
                                Text(last.address, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = onReconnect) { Text("重新连接") }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("启动时自动尝试连接上次打印机")
                    Text(
                        "默认开启；仅在蓝牙和权限已可用时尝试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.autoReconnect, onCheckedChange = onAutoReconnectChange)
            }

            Button(onClick = if (state.isScanning) onStopScan else onSearch) {
                if (state.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("停止扫描")
                } else {
                    Text("搜索附近设备")
                }
            }
            Text(
                if (state.devices.isEmpty()) "尚未发现设备" else "附近设备 · ${state.devices.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                if (state.devices.isEmpty()) {
                    Text(
                        "点击“搜索附近设备”查找 Bluetooth Classic 和 BLE 打印机。",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.devices.forEachIndexed { index, device ->
                    val activeTransport = state.connectionInfo?.transport
                        ?: state.currentDevice?.transports?.singleOrNull()
                    DeviceRow(
                        device = device,
                        isCurrent = state.currentDevice?.address == device.address &&
                            device.transports.singleOrNull() == activeTransport,
                        status = state.connectionStatus,
                        onConnect = { onConnect(device) },
                    )
                    if (index != state.devices.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CurrentDeviceCard(
    state: MainUiState,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    val connected = state.connectionStatus == ConnectionStatus.CONNECTED
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusCircle(state.connectionStatus)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("当前设备", style = MaterialTheme.typography.labelMedium)
                Text(
                    state.currentDevice?.displayName ?: "未连接",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.currentDevice?.address ?: "未选择设备",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    state.connectionStatus.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (state.connectionStatus) {
                        ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                        ConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                state.connectionInfo?.let { info ->
                    Text(
                        "连接方式：${info.transport.connectionLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (connected || state.connectionStatus == ConnectionStatus.CONNECTING) {
                OutlinedButton(onClick = onDisconnect) { Text("断开") }
            } else if (state.currentDevice != null || state.lastDevice != null) {
                OutlinedButton(onClick = onReconnect) { Text("重新连接") }
            }
        }
    }
}

@Composable
private fun StatusCircle(status: ConnectionStatus) {
    val color = when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
        ConnectionStatus.CONNECTING, ConnectionStatus.DISCONNECTING -> Color(0xFFED8B00)
        ConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
        ConnectionStatus.DISCONNECTED -> Color(0xFF858B88)
    }
    Box(Modifier.size(12.dp).background(color, CircleShape))
}

@Composable
private fun DeviceRow(
    device: PrinterDevice,
    isCurrent: Boolean,
    status: ConnectionStatus,
    onConnect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    device.displayName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                )
                device.transports.singleOrNull()?.let { TransportBadge(it) }
            }
            Text(device.address, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = onConnect,
            enabled = !(isCurrent && status in setOf(ConnectionStatus.CONNECTED, ConnectionStatus.CONNECTING)),
        ) {
            Text(if (isCurrent && status == ConnectionStatus.CONNECTED) "已连接" else "连接")
        }
    }
}

@Composable
private fun TransportBadge(transport: BluetoothTransport) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            "[${transport.badgeLabel}]",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
