package com.wanlian.printer.printing

import com.wanlian.printer.storage.DiagnosticLogRepository
import java.io.IOException

enum class PrintUploadStage(val label: String) {
    PREPARING("生成打印数据"),
    PAGE_SETUP("发送纸张尺寸"),
    SETTINGS("发送浓度和速度"),
    BITMAP_HEADER("发送位图头"),
    BITMAP_DATA("上传位图数据"),
    PRINT_COMMAND("发送 PRINT 命令"),
}

class PrintUploadException(
    val stage: PrintUploadStage,
    val sentBytes: Int,
    val totalBytes: Int,
    val completedChunks: Int,
    val totalChunks: Int,
    val transportLabel: String,
    cause: Throwable,
) : IOException(buildMessage(stage, sentBytes, totalBytes, transportLabel, cause), cause) {
    val progressPercent: Int
        get() = if (totalBytes <= 0) 0 else (sentBytes * 100L / totalBytes).toInt().coerceIn(0, 100)

    companion object {
        private fun buildMessage(
            stage: PrintUploadStage,
            sentBytes: Int,
            totalBytes: Int,
            transportLabel: String,
            cause: Throwable,
        ): String {
            val progress = if (totalBytes > 0) {
                "，位图已发送 $sentBytes / $totalBytes 字节（${sentBytes * 100L / totalBytes}%）"
            } else {
                ""
            }
            return "${stage.label}失败$progress，通道 $transportLabel：" +
                DiagnosticLogRepository.rootCauseMessage(cause)
        }
    }
}
