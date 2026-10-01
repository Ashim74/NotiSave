package com.droidnova.notificationhistory.core.apps

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Process-wide cache of app labels and rasterized launcher icons keyed by package name.
 *
 * PackageManager lookups are IPC calls and adaptive icons rasterize to ~0.7 MB each at
 * intrinsic size, so every list row resolving its own icon was both slow and memory-heavy.
 * Icons are downscaled to [ICON_PX] (plenty for the 24–48 dp they are drawn at) and kept
 * in a byte-bounded LRU.
 */
object AppInfoCache {

    private const val ICON_PX = 96
    private const val MAX_ICON_CACHE_BYTES = 6 * 1024 * 1024

    private val labels = ConcurrentHashMap<String, String>()
    private val missingIcons: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val icons = object : LruCache<String, ImageBitmap>(MAX_ICON_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** Cached label. The first call per package hits PackageManager, so call off the main thread. */
    fun label(pm: PackageManager, packageName: String): String =
        labels.getOrPut(packageName) {
            runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrElse { packageName }
        }

    /** Non-blocking: returns the icon only if already rasterized. */
    fun peekIcon(packageName: String): ImageBitmap? = icons.get(packageName)

    /** Blocking: resolves and rasterizes the icon. Call from Dispatchers.IO. */
    fun loadIcon(pm: PackageManager, packageName: String): ImageBitmap? {
        icons.get(packageName)?.let { return it }
        if (packageName in missingIcons) return null
        val bitmap = runCatching {
            pm.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
        }.getOrNull()
        if (bitmap == null) missingIcons += packageName else icons.put(packageName, bitmap)
        return bitmap
    }

    suspend fun icon(context: Context, packageName: String): ImageBitmap? =
        peekIcon(packageName)
            ?: withContext(Dispatchers.IO) { loadIcon(context.packageManager, packageName) }

    /**
     * Forget negative results so apps installed since the last lookup resolve again.
     * Call whenever the installed-app list is refreshed.
     */
    fun invalidateMisses() {
        missingIcons.clear()
        // An unresolved label is cached as the package name itself.
        labels.entries.removeIf { (pkg, label) -> label == pkg }
    }

    fun clear() {
        labels.clear()
        missingIcons.clear()
        icons.evictAll()
    }
}
