package com.wanlian.printer.model

object TemplateTransferRules {
    const val FILE_EXTENSION = "wanlian"
    const val MIME_TYPE = "application/vnd.wanlian.template+json"
    const val FORMAT_ID = "wanlian-template"
    const val FORMAT_VERSION = 1
    const val MAX_IMPORT_BYTES = 2 * 1024 * 1024

    fun uniqueImportedName(sourceName: String, existingNames: Collection<String>): String {
        val normalizedSource = TemplateNameRules.normalize(sourceName).ifBlank { "导入模板" }
        val occupied = existingNames.mapTo(hashSetOf()) { TemplateNameRules.normalize(it) }
        if (normalizedSource !in occupied) return normalizedSource

        var index = 1
        while (true) {
            val suffix = if (index == 1) "（导入）" else "（导入 $index）"
            val sourceLimit = (TemplateNameRules.MAX_CODE_POINTS - suffix.codePointCount(0, suffix.length))
                .coerceAtLeast(1)
            val base = limitCodePoints(normalizedSource, sourceLimit)
            val candidate = TemplateNameRules.normalize(base + suffix)
            if (candidate !in occupied) return candidate
            index++
        }
    }

    fun safeFileStem(name: String): String {
        val normalized = TemplateNameRules.normalize(name).ifBlank { "挽联模板" }
        val sanitized = normalized.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        return sanitized.trim('.', ' ').ifBlank { "挽联模板" }
    }

    private fun limitCodePoints(value: String, maximum: Int): String {
        if (value.codePointCount(0, value.length) <= maximum) return value
        return value.substring(0, value.offsetByCodePoints(0, maximum))
    }
}
