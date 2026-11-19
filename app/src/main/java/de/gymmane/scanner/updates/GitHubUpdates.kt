package de.gymmane.scanner.updates

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import de.gymmane.scanner.BuildConfig
import de.gymmane.scanner.data.awaitResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GitHubFailure(val code: Int, val retryAt: Long = 0) : IOException()

class GitHubUpdates(private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS).callTimeout(2, TimeUnit.MINUTES).build(),
    private val apiRoot: String = "https://api.github.com") {
    private val downloadMutex = Mutex()
    @Volatile private var downloadCall: Call? = null
    fun cancelDownload() { downloadCall?.cancel() }
    suspend fun release(path: String = "latest"): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiRoot/repos/${UpdateConfig.REPOSITORY}/releases/$path")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GymManeScanner/${BuildConfig.VERSION_NAME}").build()
        client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) {
                val reset = response.header("X-RateLimit-Reset")?.toLongOrNull()?.times(1000) ?: 0
                val retry = response.header("Retry-After")?.toLongOrNull()?.let { System.currentTimeMillis() + it * 1000 } ?: 0
                throw GitHubFailure(response.code, maxOf(reset, retry))
            }
            val body = response.body ?: throw IOException()
            if (body.contentLength() > 2_000_000) throw IOException()
            // Bound even chunked responses before JSON parsing.
            val bytes = body.byteStream().use { it.readBytesBounded(2_000_000) }
            JSONObject(bytes.toString(Charsets.UTF_8))
        }
    }

    suspend fun download(release: Release, folder: File, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
      downloadMutex.withLock {
        folder.mkdirs()
        val partial = File(folder, "update.apk.part")
        val target = File(folder, "update.apk")
        try {
            val request = Request.Builder().url(release.assetUrl).header("User-Agent", "GymManeScanner/${BuildConfig.VERSION_NAME}").build()
            val call = client.newBuilder().callTimeout(10, TimeUnit.MINUTES).build().newCall(request)
            downloadCall = call
            call.awaitResponse().use { response ->
                if (!response.isSuccessful) throw IOException()
                val body = response.body ?: throw IOException()
                val digest = MessageDigest.getInstance("SHA-256")
                var received = 0L; var lastProgress = 0L
                body.byteStream().use { input -> partial.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > release.size || received > 536870912) throw IOException()
                        digest.update(buffer, 0, count); output.write(buffer, 0, count)
                        val now = System.nanoTime()
                        if (now - lastProgress > 100_000_000) {
                            progress(received.toFloat() / release.size); lastProgress = now
                        }
                    }
                } }
                if (received != release.size) throw IOException()
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (release.digest != null && !hash.equals(release.digest.removePrefix("sha256:"), true)) throw IOException()
            }
            if (!partial.renameTo(target)) throw IOException()
            progress(1f)
            target
        } finally { partial.delete(); downloadCall = null }
      }
    }
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        if (out.size() + n > limit) throw IOException()
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}

/** Reject another app, downgrades and incompatible signing keys before invoking the installer. */
@Suppress("DEPRECATION")
fun validateUpdateApk(context: Context, file: File, version: SemVer) {
    val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val pm = context.packageManager
    val incoming = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: throw IOException()
    val installed = pm.getPackageInfo(context.packageName, flags)
    val code = if (Build.VERSION.SDK_INT >= 28) incoming.longVersionCode else incoming.versionCode.toLong()
    val currentCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
    if (incoming.packageName != context.packageName || SemVer.parse(incoming.versionName.orEmpty()) != version || code <= currentCode) throw IOException()
    fun signatures(info: android.content.pm.PackageInfo): Set<String> =
        (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            ?.map { it.toCharsString() }?.toSet().orEmpty()
    val old = signatures(installed)
    val next = signatures(incoming)
    val history = if (Build.VERSION.SDK_INT >= 28 && incoming.signingInfo?.hasMultipleSigners() == false)
        incoming.signingInfo?.signingCertificateHistory?.map { it.toCharsString() }?.toSet().orEmpty() else emptySet()
    if (old.isEmpty() || next.isEmpty() || (old != next && !(old.size == 1 && history.containsAll(old)))) throw IOException()
}
