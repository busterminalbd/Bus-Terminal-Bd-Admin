package com.example.data.network

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads a release .apk through Android's own DownloadManager (so the user sees the
 * normal system download notification/progress, same as any other file download) and, once
 * it finishes, hands the file straight to the system package installer.
 *
 * This is the "update via file" flow: no Play Store, no browser tab left for the user to
 * dig through — tap "Download update", wait for the download-complete notification (or the
 * install screen, which opens automatically), tap Install.
 */
object ApkUpdateDownloader {

    private const val FILE_NAME = "bus-terminal-bd-admin-update.apk"

    /**
     * Starts the background download. If the device hasn't yet granted this app permission
     * to install unknown apps, [onNeedsInstallPermission] is invoked instead so the caller can
     * send the user to the one-time system settings screen; call this function again afterwards.
     */
    fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onNeedsInstallPermission: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            onNeedsInstallPermission()
            return
        }

        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val destinationFile = File(destinationDir, FILE_NAME)
        if (destinationFile.exists()) destinationFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("বাস টার্মিনাল বিডি অ্যাডমিন — আপডেট")
            .setDescription("নতুন সংস্করণ ডাউনলোড হচ্ছে...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val appContext = context.applicationContext

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != downloadId) return
                try {
                    appContext.unregisterReceiver(this)
                } catch (_: Exception) {
                    // already unregistered — fine
                }

                if (!destinationFile.exists() || destinationFile.length() == 0L) {
                    Toast.makeText(
                        appContext,
                        "আপডেট ডাউনলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                installApk(appContext, destinationFile)
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }

        Toast.makeText(appContext, "আপডেট ডাউনলোড শুরু হয়েছে...", Toast.LENGTH_SHORT).show()
    }

    private fun installApk(context: Context, file: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
