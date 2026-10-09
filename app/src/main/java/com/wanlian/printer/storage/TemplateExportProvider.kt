package com.wanlian.printer.storage

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 跨应用模板迁移接口。
 *
 * 旧版（包名 [LEGACY_PACKAGE]）升级安装后，本 Provider 会把其本地 DataStore 中的全部模板
 * 以 JSON 形式暴露给“修复版”（不同包名）读取。修复版通过 [MainViewModel.migrateLegacyTemplates]
 * 调用 `content://com.wanlian.printer.templates` 的 [METHOD_EXPORT_TEMPLATES] 方法一次拉取全部模板。
 *
 * 说明：模板数据为非敏感数据，Provider 设为 exported=true 以便跨包读取；如需收紧，
 * 可改为 signature 级自定义权限（两个 APK 同签时可用）。
 */
class TemplateExportProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_EXPORT_TEMPLATES) return null
        val appContext = context ?: return null
        val repository = TemplateRepository(appContext)
        val json = runBlocking { repository.allTemplatesJson() }
        return Bundle().apply { putString(KEY_TEMPLATES_JSON, json) }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        /** 迁移源（旧版）的固定包名。 */
        const val LEGACY_PACKAGE = "com.wanlian.printer"

        /** Provider 授权后缀；完整授权为 `${applicationId}${AUTHORITY_SUFFIX}`。 */
        const val AUTHORITY_SUFFIX = ".templates"

        /** 旧版（迁移源）的完整 Provider 授权。 */
        val LEGACY_AUTHORITY: String get() = "$LEGACY_PACKAGE$AUTHORITY_SUFFIX"

        const val METHOD_EXPORT_TEMPLATES = "export_templates"
        const val KEY_TEMPLATES_JSON = "templates_json"
    }
}
