package com.droidnova.notificationhistory.data.backup

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.droidnova.notificationhistory.core.apps.AppInfoCache
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.data.db.AppDatabase
import com.droidnova.notificationhistory.data.db.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium data portability: a spreadsheet export (CSV) and a full backup (JSON) that restores on
 * any phone. Rows stream in chunks, so a large history never has to fit in memory at once.
 * A backup carries the saved notifications plus the choices that shape capture (selected apps,
 * title filters, block words, keyword alerts, hidden apps); restoring merges, never replaces.
 */
class BackupManager(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).notificationDao()
    private val prefs = UserPreferences(context)

    /** Writes every active notification as CSV to [uri]; returns how many rows were written. */
    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val stream = openForWrite(uri)
        var count = 0
        stream.bufferedWriter().use { out ->
            // A byte-order mark makes Excel read the file as UTF-8 (emoji, non-Latin scripts).
            out.write("﻿")
            out.write(CSV_HEADER)
            out.newLine()
            forEachActiveRow { row ->
                val fields = listOf(
                    formatter.format(Date(row.receivedAt)),
                    AppInfoCache.label(context.packageManager, row.packageName),
                    row.packageName,
                    row.title,
                    row.message,
                    row.deletedAt?.let { formatter.format(Date(it)) }.orEmpty()
                )
                out.write(fields.joinToString(",", transform = ::csvField))
                out.newLine()
                count++
            }
        }
        count
    }

    /** Writes a full JSON backup to [uri]; returns how many notifications it holds. */
    suspend fun backup(uri: Uri): Int = withContext(Dispatchers.IO) {
        val stream = openForWrite(uri)
        var count = 0
        JsonWriter(stream.bufferedWriter()).use { json ->
            json.beginObject()
            json.name(KEY_FORMAT).value(FORMAT)
            json.name(KEY_VERSION).value(VERSION)
            json.name(KEY_CREATED_AT).value(System.currentTimeMillis())
            json.name(KEY_ALLOWED_APPS).stringArray(prefs.allowedApps.first())
            json.name(KEY_KEYWORDS).stringArray(prefs.keywordAlerts.first())
            json.name(KEY_HIDDEN_APPS).stringArray(prefs.hiddenApps.first())
            json.name(KEY_TITLE_FILTERS).stringSetMap(prefs.allTitleFilters.first())
            json.name(KEY_BLOCK_WORDS).stringSetMap(prefs.allBlockWords.first())
            json.name(KEY_NOTIFICATIONS).beginArray()
            forEachActiveRow { row ->
                json.beginObject()
                json.name("packageName").value(row.packageName)
                json.name("title").value(row.title)
                json.name("message").value(row.message)
                json.name("receivedAt").value(row.receivedAt)
                json.name("notificationKey").value(row.notificationKey)
                json.name("contentFingerprint").value(row.contentFingerprint)
                json.name("conversationTitle").value(row.conversationTitle)
                json.name("conversationKey").value(row.conversationKey)
                json.name("conversationName").value(row.conversationName)
                row.deletedAt?.let { json.name("deletedAt").value(it) }
                json.endObject()
                count++
            }
            json.endArray()
            json.endObject()
        }
        count
    }

    /**
     * Merges the backup at [uri] into this phone: notifications already here (same app, time,
     * title and text) are skipped, preference sets are unioned. Returns how many were added.
     * Throws [BackupFormatException] for a file that is not a NotiSave backup.
     */
    suspend fun restore(uri: Uri): Int = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("No input for $uri")
        var added = 0
        var sawFormat = false
        JsonReader(stream.bufferedReader()).use { json ->
            json.beginObject()
            while (json.hasNext()) {
                when (json.nextName()) {
                    KEY_FORMAT -> sawFormat = json.nextString() == FORMAT
                    KEY_ALLOWED_APPS -> json.readStrings().let { apps -> prefs.setAllowedApps(prefs.allowedApps.first() + apps) }
                    KEY_KEYWORDS -> json.readStrings().forEach { prefs.addKeywordAlert(it) }
                    KEY_HIDDEN_APPS -> json.readStrings().forEach { prefs.setAppHidden(it, hidden = true) }
                    KEY_TITLE_FILTERS -> json.readStringSetMap().forEach { (pkg, words) ->
                        words.forEach { prefs.addTitleFilter(pkg, it) }
                    }
                    KEY_BLOCK_WORDS -> json.readStringSetMap().forEach { (pkg, words) ->
                        words.forEach { prefs.addBlockWord(pkg, it) }
                    }
                    KEY_NOTIFICATIONS -> {
                        if (!sawFormat) throw BackupFormatException()
                        added = readNotifications(json)
                    }
                    else -> json.skipValue()
                }
            }
            json.endObject()
        }
        if (!sawFormat) throw BackupFormatException()
        added
    }

    private suspend fun readNotifications(json: JsonReader): Int {
        var added = 0
        val batch = ArrayList<NotificationEntity>(CHUNK)
        suspend fun flush() {
            if (batch.isEmpty()) return
            dao.insertAll(batch)
            added += batch.size
            batch.clear()
        }
        json.beginArray()
        while (json.hasNext()) {
            val row = json.readNotification() ?: continue
            if (!dao.hasExactRow(row.packageName, row.receivedAt, row.title, row.message)) {
                batch += row
                if (batch.size >= CHUNK) flush()
            }
        }
        json.endArray()
        flush()
        return added
    }

    /** Truncating mode first (so a shorter file never keeps old bytes); not every provider has it. */
    private fun openForWrite(uri: Uri) =
        runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: context.contentResolver.openOutputStream(uri)
            ?: throw IOException("No output for $uri")

    private suspend fun forEachActiveRow(action: (NotificationEntity) -> Unit) {
        var afterId = 0L
        while (true) {
            val chunk = dao.getActiveChunk(afterId, CHUNK)
            chunk.forEach(action)
            if (chunk.size < CHUNK) return
            afterId = chunk.last().id
        }
    }

    private fun JsonReader.readNotification(): NotificationEntity? {
        var packageName: String? = null
        var title = ""
        var message = ""
        var receivedAt: Long? = null
        var notificationKey: String? = null
        var contentFingerprint: String? = null
        var conversationTitle: String? = null
        var conversationKey: String? = null
        var conversationName: String? = null
        var deletedAt: Long? = null
        beginObject()
        while (hasNext()) {
            val name = nextName()
            if (peek() == JsonToken.NULL) {
                nextNull()
                continue
            }
            when (name) {
                "packageName" -> packageName = nextString()
                "title" -> title = nextString()
                "message" -> message = nextString()
                "receivedAt" -> receivedAt = nextLong()
                "notificationKey" -> notificationKey = nextString()
                "contentFingerprint" -> contentFingerprint = nextString()
                "conversationTitle" -> conversationTitle = nextString()
                "conversationKey" -> conversationKey = nextString()
                "conversationName" -> conversationName = nextString()
                "deletedAt" -> deletedAt = nextLong()
                else -> skipValue()
            }
        }
        endObject()
        return NotificationEntity(
            packageName = packageName ?: return null,
            title = title,
            message = message,
            receivedAt = receivedAt ?: return null,
            notificationKey = notificationKey,
            contentFingerprint = contentFingerprint,
            conversationTitle = conversationTitle,
            conversationKey = conversationKey,
            conversationName = conversationName,
            deletedAt = deletedAt
        )
    }

    private fun JsonWriter.stringArray(values: Collection<String>) {
        beginArray()
        values.forEach { value(it) }
        endArray()
    }

    private fun JsonWriter.stringSetMap(map: Map<String, Set<String>>) {
        beginObject()
        map.forEach { (key, values) -> name(key).stringArray(values) }
        endObject()
    }

    private fun JsonReader.readStrings(): List<String> {
        val result = mutableListOf<String>()
        beginArray()
        while (hasNext()) result += nextString()
        endArray()
        return result
    }

    private fun JsonReader.readStringSetMap(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        beginObject()
        while (hasNext()) {
            val key = nextName()
            result[key] = readStrings()
        }
        endObject()
        return result
    }

    private companion object {
        const val CHUNK = 500
        const val FORMAT = "notisave-backup"
        const val VERSION = 1
        const val KEY_FORMAT = "format"
        const val KEY_VERSION = "version"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_ALLOWED_APPS = "allowedApps"
        const val KEY_KEYWORDS = "keywordAlerts"
        const val KEY_HIDDEN_APPS = "hiddenApps"
        const val KEY_TITLE_FILTERS = "titleFilters"
        const val KEY_BLOCK_WORDS = "blockWords"
        const val KEY_NOTIFICATIONS = "notifications"
        const val CSV_HEADER = "Date,App,Package,Title,Message,Deleted at"
    }
}

/** The chosen file is not a NotiSave backup (or is damaged). */
class BackupFormatException : IOException("Not a NotiSave backup")

/** Quotes a CSV field when needed, doubling inner quotes (RFC 4180). */
internal fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
