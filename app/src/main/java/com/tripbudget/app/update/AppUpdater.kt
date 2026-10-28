package com.tripbudget.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tripbudget.app.cloud.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
)

/** Checks Supabase for a newer Chilavu build. The release page opens in the browser. */
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
        if (versionName.isEmpty() || releaseUri(apkUrl) == null) return@withContext null
        AppRelease(versionCode = latest.versionCode, versionName = versionName, apkUrl = apkUrl)
    }

    fun openRelease(context: Context, apkUrl: String) {
        val uri = releaseUri(apkUrl) ?: error("update")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun releaseUri(raw: String): Uri? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val path = uri.path ?: return null
        val allowed = uri.scheme == "https" &&
            uri.host == "github.com" &&
            path.startsWith(RELEASE_PREFIX) &&
            path.endsWith(".apk") &&
            !path.contains("..")
        if (!allowed) return null
        return Uri.parse(uri.toString())
    }
}

@Serializable
private data class RemoteAppRelease(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("apk_url") val apkUrl: String? = null,
)
