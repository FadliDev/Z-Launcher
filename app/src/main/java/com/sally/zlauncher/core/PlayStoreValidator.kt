package com.sally.zlauncher.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object McPackage {
    const val NAME = "com.mojang.minecraftpe"
}

enum class McInstallState { NOT_INSTALLED, PLAY_STORE, OTHER_SOURCE }

/** Memeriksa apakah Minecraft terpasang dan dipasang oleh Google Play (bukti lisensi). */
object PlayStoreValidator {

    private const val PLAY_STORE = "com.android.vending"

    @Suppress("DEPRECATION")
    fun state(context: Context): McInstallState {
        val pm = context.packageManager
        try {
            pm.getPackageInfo(McPackage.NAME, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return McInstallState.NOT_INSTALLED
        }

        val installer: String? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(McPackage.NAME).installingPackageName
            } else {
                pm.getInstallerPackageName(McPackage.NAME)
            }
        } catch (e: Exception) {
            null
        }
        return if (installer == PLAY_STORE) McInstallState.PLAY_STORE else McInstallState.OTHER_SOURCE
    }

    @Suppress("DEPRECATION")
    fun installedVersionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(McPackage.NAME, 0).versionName
    } catch (e: Exception) {
        null
    }
}
