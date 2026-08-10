package com.wanlian.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.ui.PrinterApp
import com.wanlian.printer.ui.WanlianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WanlianTheme {
                val mainViewModel: MainViewModel = viewModel()
                BluetoothPermissionHost(mainViewModel)
            }
        }
    }
}

@Composable
private fun BluetoothPermissionHost(viewModel: MainViewModel) {
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val action = pendingAction
        if (viewModel.isBluetoothEnabled) {
            pendingAction = null
            action?.invoke()
        } else {
            pendingAction = null
            viewModel.reportMessage("蓝牙未开启，无法搜索或连接打印机")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val allGranted = result.values.all { it }
        if (!allGranted) {
            pendingAction = null
            viewModel.reportMessage("蓝牙权限被拒绝，请在系统设置中允许“附近的设备”权限")
        } else if (!viewModel.isBluetoothEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            val action = pendingAction
            pendingAction = null
            action?.invoke()
        }
    }

    fun runWhenBluetoothReady(action: () -> Unit) {
        if (!viewModel.isBluetoothAvailable) {
            viewModel.reportMessage("此 Android 设备不支持蓝牙")
            return
        }
        val required = requiredBluetoothPermissions()
        val missing = required.filter {
            ContextCompat.checkSelfPermission(
                viewModel.getApplication(),
                it,
            ) != PackageManager.PERMISSION_GRANTED
        }
        pendingAction = action
        when {
            missing.isNotEmpty() -> permissionLauncher.launch(missing.toTypedArray())
            !viewModel.isBluetoothEnabled ->
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> {
                pendingAction = null
                action()
            }
        }
    }

    PrinterApp(
        viewModel = viewModel,
        onSearch = { runWhenBluetoothReady(viewModel::startScan) },
        onConnect = { device: PrinterDevice ->
            runWhenBluetoothReady { viewModel.connect(device) }
        },
        onReconnect = { runWhenBluetoothReady(viewModel::reconnect) },
    )
}

private fun requiredBluetoothPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
