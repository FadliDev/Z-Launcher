package com.sally.zlauncher.data

/**
 * Satu entri versi Minecraft di launcher.
 *
 * @param id          nama folder penyimpanan (atau [INSTALLED_ID] untuk game yang terpasang)
 * @param isInstalled true = Minecraft yang terpasang dari Google Play, false = hasil impor APK
 */
data class GameVersion(
    val id: String,
    val displayName: String,
    val versionName: String,
    val isInstalled: Boolean,
    val sizeBytes: Long = 0L,
    val createdAt: Long = 0L
) {
    companion object {
        const val INSTALLED_ID = "__installed__"
    }
}
