package com.droidnova.notificationhistory.data_shared

/**
 * The accent palette picked in Settings, applied on top of [ThemeMode] (light or dark). The first
 * three match Secret Calculator's free themes; [isPro] ones need Premium.
 */
enum class ColorTheme(val storageKey: String, val isPro: Boolean = false) {
    Teal("teal"),
    Blue("blue"),
    Pink("pink"),
    Purple("purple", isPro = true),
    Midnight("midnight", isPro = true),
    Sunset("sunset", isPro = true);

    companion object {
        val Default = Teal

        fun fromStorageKey(key: String?): ColorTheme =
            entries.firstOrNull { it.storageKey == key } ?: Default
    }
}
