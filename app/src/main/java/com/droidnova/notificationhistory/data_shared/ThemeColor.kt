package com.droidnova.notificationhistory.data_shared

/** The accent color the app is painted in, independent of light / dark. [pro] ones need Premium. */
enum class ThemeColor(val storageKey: String, val pro: Boolean) {
    Teal("teal", pro = false),
    Blue("blue", pro = false),
    Forest("forest", pro = true),
    Purple("purple", pro = true),
    Rose("rose", pro = true),
    Sunset("sunset", pro = true),
    Gold("gold", pro = true);

    companion object {
        fun fromStorageKey(key: String?): ThemeColor =
            entries.firstOrNull { it.storageKey == key } ?: Teal
    }
}
