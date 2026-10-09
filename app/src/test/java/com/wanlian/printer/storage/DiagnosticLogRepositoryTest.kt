package com.wanlian.printer.storage

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiagnosticLogRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun migratesByDateAndClearsOnlySelectedDate() {
        val root = temporary.newFolder()
        File(root, "print-diagnostics.log").writeText(
            "2025-01-01 10:00:00.000 | TEST | first\n" +
                "2025-01-02 11:00:00.000 | TEST | another day\n" +
                "2025-01-01 12:00:00.000 | TEST | last\n"
        )
        val logs = DiagnosticLogRepository(root)
        assertTrue(logs.read("2025-01-01").lines().first().endsWith("last"))
        assertEquals(2, logs.read("2025-01-01").lines().size)
        logs.clear("2025-01-01")
        assertEquals("", logs.read("2025-01-01"))
        assertTrue(logs.read("2025-01-02").contains("another day"))
        assertEquals("", DiagnosticLogRepository(root).read("2025-01-01"))
    }

    @Test fun appendsNewestFirstWithoutBreakingMultilineRecords() {
        val logs = DiagnosticLogRepository(temporary.newFolder())
        logs.append("TEST", "first")
        logs.append("TEST", "second\nline")
        val lines = logs.read().lines()
        assertEquals(2, lines.size)
        assertTrue(lines.first().endsWith("second | line"))
        assertTrue(lines.last().endsWith("first"))
        assertEquals("", logs.read("2020-01-01"))
    }
}
