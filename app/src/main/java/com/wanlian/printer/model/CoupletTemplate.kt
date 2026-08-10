package com.wanlian.printer.model

data class CoupletTemplate(
    val id: String,
    val name: String,
    val settings: PrintSettings,
    val updatedAt: Long,
)

data class DevicePreferences(
    val lastDevice: PrinterDevice? = null,
    val autoReconnect: Boolean = true,
)
