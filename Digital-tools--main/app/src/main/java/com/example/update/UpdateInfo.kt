package com.example.update

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val currentVersionName: String = "",
    val publishedAt: String = "",
    val apkSizeFormatted: String = "",
    val errorMessage: String? = null
)
