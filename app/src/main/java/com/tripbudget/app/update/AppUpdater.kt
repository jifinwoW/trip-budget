package com.tripbudget.app.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.tripbudget.app.cloud.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
)

/** Checks Supabase for a newer Chilavu build and downloads that APK from GitHub. */
object AppUpdater {
    private const val RELEASE_PREFIX = "/jifinwoW/trip-budget/releases/download/"

    suspend fun newerRelease(installedVersionCode: Int): AppRelease? = withContext(Dispatchers.IO) {
        val supabase = SupabaseProvider.client ?: return@withContext null
        val latest = runCatching {
            supabase.from("app_release").select().decodeList<RemoteAppRelease>()
        }.getOrNull().orEmpty().maxByOrNull { it.versionCode } ?: return@withContext null
        if (latest.versionCode <= installedVersionCode) return@withContext null
        val versionName = latest.versionName.trim()
        val apkUrl = latest.apkUrl?.trim().orEmpty()
        if (versionName.isEmpty() || releaseUrl(apkUrl) == null) return@withContext null
        AppRelease(versionCode = latest.versionCode, versionName = versionName, apkUrl = apkUrl)
    }

    fun canRequestInstall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun download(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(dir, "chilavu-update.apk.part")
        val dest = File(dir, "chilavu-update.apk")
        partial.delete()
        val connection = openApkConnection(apkUrl)
        try {
            val total = connection.contentLengthLong.takeIf { it > 0L }
                ?: connection.getHeaderField("Content-Length")?.toLongOrNull()
                ?: -1L
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var received = 0L
                    var lastPercent = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        received += count
                        if (total > 0L) {
                            val percent = ((received * 100) / total).toInt().coerceIn(0, 99)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
            onProgress(1f)
        } finally {
            connection.disconnect()
        }
        if (!looksLikeApk(partial)) {
            partial.delete()
            error("update")
        }
        if (dest.exists() && !dest.delete()) error("update")
        if (!partial.renameTo(dest)) error("update")
        dest
    }

    fun openInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("Chilavu update", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openApkConnection(apkUrl: String): HttpURLConnection {
        var current = releaseUrl(apkUrl) ?: error("update")
        repeat(5) {
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "Chilavu")
            }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                if (!allowedHost(connection.url)) {
                    connection.disconnect()
                    error("update")
                }
                return connection
            }
            if (code !in 300..399) {
                connection.disconnect()
                error("update")
            }
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            if (location.isNullOrBlank()) error("update")
            val next = URI(current.toString()).resolve(location).toURL()
            if (!allowedHost(next)) error("update")
            current = next
        }
        error("update")
    }

    private fun releaseUrl(raw: String): URL? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val path = uri.path ?: return null
        val allowed = uri.scheme == "https" &&
            uri.host == "github.com" &&
            path.startsWith(RELEASE_PREFIX) &&
            path.endsWith(".apk") &&
            !path.contains("..")
        if (!allowed) return null
        return uri.toURL()
    }

    private fun allowedHost(url: URL): Boolean {
        val host = url.host ?: return false
        return host == "github.com" || host.endsWith(".githubusercontent.com")
    }

    private fun looksLikeApk(file: File): Boolean {
        if (file.length() < 1024) return false
        val magic = ByteArray(2)
        file.inputStream().use { input ->
            if (input.read(magic) != 2) return false
        }
        return magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte()
    }
}

@Serializable
private data class RemoteAppRelease(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("apk_url") val apkUrl: String? = null,
)
