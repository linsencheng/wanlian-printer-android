package com.wanlian.printer.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wanlian.printer.model.BluetoothTransport
import com.wanlian.printer.model.BorderPosition
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderStyle
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.DevicePreferences
import com.wanlian.printer.model.PrintDirection
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.model.TextHorizontalAlignment
import com.wanlian.printer.model.TextWeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private val Context.wanlianDataStore by preferencesDataStore(name = "wanlian_local_data")

class TemplateRepository(context: Context) {
    private val dataStore = context.applicationContext.wanlianDataStore

    val templates: Flow<List<CoupletTemplate>> = safeData().map { preferences ->
        parseTemplates(preferences[TEMPLATES_JSON].orEmpty())
    }

    val devicePreferences: Flow<DevicePreferences> = safeData().map { preferences ->
        DevicePreferences(
            lastDevice = parseDevice(preferences[LAST_DEVICE_JSON]),
            autoReconnect = preferences[AUTO_RECONNECT] ?: true,
        )
    }

    suspend fun saveTemplate(name: String, settings: PrintSettings, id: String? = null) {
        dataStore.edit { preferences ->
            val current = parseTemplates(preferences[TEMPLATES_JSON].orEmpty()).toMutableList()
            val templateId = id ?: UUID.randomUUID().toString()
            current.removeAll { it.id == templateId }
            current += CoupletTemplate(
                id = templateId,
                name = name.trim().ifEmpty { "未命名模板" },
                settings = settings,
                updatedAt = System.currentTimeMillis(),
            )
            preferences[TEMPLATES_JSON] = templatesToJson(current.sortedByDescending { it.updatedAt })
        }
    }

    suspend fun deleteTemplate(id: String) {
        dataStore.edit { preferences ->
            val remaining = parseTemplates(preferences[TEMPLATES_JSON].orEmpty())
                .filterNot { it.id == id }
            preferences[TEMPLATES_JSON] = templatesToJson(remaining)
        }
    }

    suspend fun saveLastDevice(device: PrinterDevice) {
        dataStore.edit { preferences ->
            preferences[LAST_DEVICE_JSON] = deviceToJson(device).toString()
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[AUTO_RECONNECT] = enabled }
    }

    private fun safeData(): Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    private fun templatesToJson(templates: List<CoupletTemplate>): String = JSONArray().apply {
        templates.forEach { template ->
            put(
                JSONObject()
                    .put("id", template.id)
                    .put("name", template.name)
                    .put("updatedAt", template.updatedAt)
                    .put("settings", settingsToJson(template.settings)),
            )
        }
    }.toString()

    private fun parseTemplates(json: String): List<CoupletTemplate> = runCatching {
        val array = if (json.isBlank()) JSONArray() else JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    CoupletTemplate(
                        id = item.getString("id"),
                        name = item.optString("name", "未命名模板"),
                        settings = settingsFromJson(item.getJSONObject("settings")),
                        updatedAt = item.optLong("updatedAt", 0L),
                    ),
                )
            }
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    private fun settingsToJson(settings: PrintSettings): JSONObject = JSONObject().apply {
        put("text", settings.text)
        put("autoFontSize", settings.autoFontSize)
        put("fontSizeDots", settings.fontSizeDots)
        put("characterSpacingDots", settings.characterSpacingDots)
        put("topMarginMm", settings.topMarginMm)
        put("bottomMarginMm", settings.bottomMarginMm)
        put("paperWidthMm", settings.paperWidthMm)
        put("autoPaperLength", settings.autoPaperLength)
        put("paperLengthMm", settings.paperLengthMm)
        put("density", settings.density)
        put("speed", settings.speedInchesPerSecond)
        put("threshold", settings.threshold)
        put("textAlignment", settings.textAlignment.name)
        put("textWeight", settings.textWeight.name)
        put("printDirection", settings.printDirection.name)
        put("reversePrinting", settings.reversePrinting)
        put("bitmapChunkSize", settings.bitmapChunkSize)
        put("chunkDelayMs", settings.chunkDelayMs)
        put("border", borderToJson(settings.border))
    }

    private fun settingsFromJson(json: JSONObject): PrintSettings {
        val defaults = PrintSettings()
        return defaults.copy(
            text = json.optString("text", defaults.text),
            autoFontSize = json.optBoolean("autoFontSize", defaults.autoFontSize),
            fontSizeDots = json.optDouble("fontSizeDots", defaults.fontSizeDots.toDouble()).toFloat(),
            characterSpacingDots = json.optDouble(
                "characterSpacingDots",
                defaults.characterSpacingDots.toDouble(),
            ).toFloat(),
            topMarginMm = json.optDouble("topMarginMm", defaults.topMarginMm.toDouble()).toFloat(),
            bottomMarginMm = json.optDouble("bottomMarginMm", defaults.bottomMarginMm.toDouble()).toFloat(),
            paperWidthMm = json.optDouble("paperWidthMm", defaults.paperWidthMm.toDouble()).toFloat(),
            autoPaperLength = json.optBoolean("autoPaperLength", defaults.autoPaperLength),
            paperLengthMm = json.optDouble("paperLengthMm", defaults.paperLengthMm.toDouble()).toFloat(),
            density = json.optInt("density", defaults.density),
            speedInchesPerSecond = json.optDouble("speed", defaults.speedInchesPerSecond.toDouble()).toFloat(),
            threshold = json.optInt("threshold", defaults.threshold),
            textAlignment = enumOrDefault(json.optString("textAlignment"), defaults.textAlignment),
            textWeight = enumOrDefault(json.optString("textWeight"), defaults.textWeight),
            printDirection = enumOrDefault(json.optString("printDirection"), defaults.printDirection),
            reversePrinting = json.optBoolean("reversePrinting", defaults.reversePrinting),
            bitmapChunkSize = json.optInt("bitmapChunkSize", defaults.bitmapChunkSize),
            chunkDelayMs = json.optLong("chunkDelayMs", defaults.chunkDelayMs),
            border = json.optJSONObject("border")?.let(::borderFromJson) ?: defaults.border,
        )
    }

    private fun borderToJson(border: BorderSettings): JSONObject = JSONObject().apply {
        put("style", border.style.name)
        put("position", border.position.name)
        put("widthMm", border.widthMm)
        put("edgeInsetMm", border.edgeInsetMm)
        put("strokeWidthMm", border.strokeWidthMm)
        put("patternUnitHeightMm", border.patternUnitHeightMm)
        put("textGapMm", border.textGapMm)
    }

    private fun borderFromJson(json: JSONObject): BorderSettings {
        val defaults = BorderSettings()
        return defaults.copy(
            style = enumOrDefault(json.optString("style"), defaults.style),
            position = enumOrDefault(json.optString("position"), defaults.position),
            widthMm = json.optDouble("widthMm", defaults.widthMm.toDouble()).toFloat(),
            edgeInsetMm = json.optDouble("edgeInsetMm", defaults.edgeInsetMm.toDouble()).toFloat(),
            strokeWidthMm = json.optDouble("strokeWidthMm", defaults.strokeWidthMm.toDouble()).toFloat(),
            patternUnitHeightMm = json.optDouble(
                "patternUnitHeightMm",
                defaults.patternUnitHeightMm.toDouble(),
            ).toFloat(),
            textGapMm = json.optDouble("textGapMm", defaults.textGapMm.toDouble()).toFloat(),
        )
    }

    private fun deviceToJson(device: PrinterDevice): JSONObject = JSONObject().apply {
        put("name", device.name)
        put("address", device.address)
        put("bonded", device.bonded)
        put("transports", JSONArray(device.transports.map { it.name }))
    }

    private fun parseDevice(json: String?): PrinterDevice? = runCatching {
        if (json.isNullOrBlank()) return@runCatching null
        val objectValue = JSONObject(json)
        val transportArray = objectValue.getJSONArray("transports")
        val transports = buildSet {
            for (index in 0 until transportArray.length()) {
                runCatching { add(BluetoothTransport.valueOf(transportArray.getString(index))) }
            }
        }
        PrinterDevice(
            name = objectValue.optString("name", ""),
            address = objectValue.getString("address"),
            transports = transports.ifEmpty { setOf(BluetoothTransport.CLASSIC) },
            bonded = objectValue.optBoolean("bonded", false),
        )
    }.getOrNull()

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    companion object {
        private val TEMPLATES_JSON = stringPreferencesKey("templates_json")
        private val LAST_DEVICE_JSON = stringPreferencesKey("last_device_json")
        private val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    }
}
