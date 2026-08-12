package com.wanlian.printer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wanlian.printer.MainViewModel
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.ui.device.DeviceScreen
import com.wanlian.printer.ui.editor.EditorScreen
import com.wanlian.printer.ui.settings.SettingsScreen

private enum class AppPage { EDITOR, DEVICE, SETTINGS }

@Composable
fun PrinterApp(
    viewModel: MainViewModel,
    onSearch: () -> Unit,
    onConnect: (PrinterDevice) -> Unit,
    onReconnect: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var page by remember { mutableStateOf(AppPage.EDITOR) }

    BackHandler(enabled = page != AppPage.EDITOR) { page = AppPage.EDITOR }
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (page) {
            AppPage.EDITOR -> EditorScreen(
                state = state,
                onSettingsChange = viewModel::updateSettings,
                onOpenDevices = { page = AppPage.DEVICE },
                onOpenSettings = {
                    viewModel.refreshDiagnosticLogs()
                    page = AppPage.SETTINGS
                },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onSaveTemplate = viewModel::saveTemplate,
                onUpdateCurrentTemplate = viewModel::updateCurrentTemplate,
                onLoadTemplate = viewModel::loadTemplate,
                onRenameTemplate = viewModel::renameTemplate,
                onDeleteTemplate = viewModel::deleteTemplate,
                onPrint = viewModel::printCouplet,
                onEnablePairMode = viewModel::enablePairMode,
                onKeepPairSideAsSingle = viewModel::keepPairSideAsSingle,
                onSelectCoupletSide = viewModel::selectCoupletSide,
                onAlignPairFooters = viewModel::alignPairFootersToSelected,
                onAlignPairClosingBlocks = viewModel::alignPairClosingBlocks,
                onRetryPairPrint = viewModel::retryPairPrint,
                onSkipFailedPairSide = viewModel::skipFailedPairSide,
                onCancelPairPrint = viewModel::cancelPairPrint,
                onAcknowledgePrintCompletion = viewModel::acknowledgePrintCompletion,
            )
            AppPage.DEVICE -> DeviceScreen(
                state = state,
                onBack = { page = AppPage.EDITOR },
                onSearch = onSearch,
                onStopScan = viewModel::stopScan,
                onConnect = onConnect,
                onDisconnect = viewModel::disconnect,
                onReconnect = onReconnect,
                onAutoReconnectChange = viewModel::setAutoReconnect,
            )
            AppPage.SETTINGS -> SettingsScreen(
                state = state,
                onBack = { page = AppPage.EDITOR },
                onOpenDevices = { page = AppPage.DEVICE },
                onSettingsChange = viewModel::updateSettings,
                onAutoReconnectChange = viewModel::setAutoReconnect,
                onPrintTest = viewModel::printTestPage,
                onPrintPolarityTest = viewModel::printPolarityTest,
                onApplyPrinterSettings = viewModel::applyPrinterSettings,
                onRefreshDiagnosticLogs = viewModel::refreshDiagnosticLogs,
                onClearDiagnosticLogs = viewModel::clearDiagnosticLogs,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        state.printFailureNotice?.let { failure ->
            AlertDialog(
                onDismissRequest = viewModel::dismissPrintFailureNotice,
                title = { Text(failure.title) },
                text = { Text(failure.reason) },
                confirmButton = {
                    Button(onClick = viewModel::dismissPrintFailureNotice) { Text("知道了") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissPrintFailureNotice()
                            viewModel.refreshDiagnosticLogs()
                            page = AppPage.SETTINGS
                        },
                    ) { Text("查看日志") }
                },
            )
        }
    }
}
