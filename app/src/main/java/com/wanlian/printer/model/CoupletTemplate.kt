package com.wanlian.printer.model

data class CoupletTemplate(
    val id: String,
    val name: String,
    val settings: PrintSettings,
    val updatedAt: Long,
    val documentMode: DocumentMode = DocumentMode.SINGLE,
    val pairDocument: CoupletPairDocument? = null,
)

object TemplateNameRules {
    const val MAX_CODE_POINTS = 30

    fun limit(value: String): String {
        if (value.codePointCount(0, value.length) <= MAX_CODE_POINTS) return value
        val endIndex = value.offsetByCodePoints(0, MAX_CODE_POINTS)
        return value.substring(0, endIndex)
    }

    fun normalize(value: String): String = limit(value.trim())

    fun isValid(value: String): Boolean = normalize(value).isNotEmpty()
}

fun CoupletTemplate.renamedTo(value: String): CoupletTemplate? {
    val normalized = TemplateNameRules.normalize(value)
    return if (normalized.isEmpty()) null else copy(name = normalized)
}

data class DevicePreferences(
    val lastDevice: PrinterDevice? = null,
    val autoReconnect: Boolean = true,
)
