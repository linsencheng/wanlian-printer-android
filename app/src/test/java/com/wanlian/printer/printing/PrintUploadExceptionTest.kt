package com.wanlian.printer.printing

import com.wanlian.printer.storage.DiagnosticLogRepository
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintUploadExceptionTest {
    @Test
    fun `reports exact upload progress transport and root cause`() {
        val root = IOException("Broken pipe")
        val wrapped = IOException("蓝牙发送失败", root)
        val error = PrintUploadException(
            stage = PrintUploadStage.BITMAP_DATA,
            sentBytes = 98_304,
            totalBytes = 102_400,
            completedChunks = 96,
            totalChunks = 100,
            transportLabel = "Bluetooth Classic (SPP)",
            cause = wrapped,
        )

        assertEquals(96, error.progressPercent)
        assertTrue(error.message.orEmpty().contains("上传位图数据"))
        assertTrue(error.message.orEmpty().contains("98304 / 102400"))
        assertTrue(error.message.orEmpty().contains("Bluetooth Classic (SPP)"))
        assertTrue(error.message.orEmpty().contains("Broken pipe"))
    }

    @Test
    fun `preserves complete exception chain for persistent diagnostics`() {
        val error = IllegalStateException("outer", IOException("socket closed"))

        assertEquals("socket closed", DiagnosticLogRepository.rootCauseMessage(error))
        assertEquals(
            "IllegalStateException: outer <- IOException: socket closed",
            DiagnosticLogRepository.describeThrowable(error),
        )
    }
}
