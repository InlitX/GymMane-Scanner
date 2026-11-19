package de.gymmane.scanner.updates

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.gymmane.scanner.BuildConfig
import de.gymmane.scanner.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class UpdateState(
    val automatic: Boolean = true, val checking: Boolean = false,
    val offer: Release? = null, val downloading: Boolean = false, val progress: Float = 0f,
    val status: Int? = null, val downloadError: Int? = null,
    val installReady: Boolean = false, val whatsNew: Boolean = false, val notes: String? = null,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("app-updates", 0)
    private val api = GitHubUpdates()
    private val folder = File(app.cacheDir, "updates")
    private val mutable = MutableStateFlow(UpdateState(automatic = prefs.getBoolean("automatic", true)))
    val state = mutable.asStateFlow()
    private var downloadJob: Job? = null
    private val installed = SemVer.parse(BuildConfig.VERSION_NAME)

    init {
        val savedVersion = prefs.getString("pending-version", null)?.let(SemVer::parse)
        if (savedVersion != null && installed != null && savedVersion <= installed) clearPending()
        val previous = prefs.getInt("last-seen-code", 0)
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        val changed = previous != BuildConfig.VERSION_CODE && (previous != 0 || info.lastUpdateTime > info.firstInstallTime)
        if (changed) {
            val cachedNotes = if (prefs.getString("notes-version", null)?.let(SemVer::parse) == installed) prefs.getString("notes", null) else null
            mutable.update { it.copy(whatsNew = true, notes = cachedNotes) }
            viewModelScope.launch { loadCurrentNotes() }
        } else {
            prefs.edit().putInt("last-seen-code", BuildConfig.VERSION_CODE).apply()
            check(false)
        }
        if (awaitingPermission && pendingFile() != null) mutable.update { it.copy(installReady = true) }
    }

    fun automatic(enabled: Boolean) {
        prefs.edit().putBoolean("automatic", enabled).apply()
        mutable.update { it.copy(automatic = enabled) }
    }

    fun check(manual: Boolean) {
        if (state.value.checking || state.value.downloading || (!manual && (!state.value.automatic || state.value.whatsNew))) return
        val now = System.currentTimeMillis()
        val blocked = prefs.getLong("blocked-until", 0)
        if (now < blocked) {
            if (manual) mutable.update { it.copy(status = R.string.update_rate_limit) }
            return
        }
        if (!manual && !CheckSchedule.due(now, prefs.getLong("last-attempt", 0), blocked)) return
        // Record attempts, not just successes. Failed automatic checks do not retry on every start.
        prefs.edit().putLong("last-attempt", now).apply()
        mutable.update { it.copy(checking = true, status = null) }
        viewModelScope.launch {
            try {
                val json = api.release()
                if (!manual && !state.value.automatic) return@launch
                val release = Release.parse(json, Build.SUPPORTED_ABIS.toList())
                if (release != null && installed != null && release.version > installed) {
                    prefs.edit().putString("notes-version", release.tag).putString("notes", release.notes).apply()
                    mutable.update { it.copy(offer = release, downloadError = null) }
                } else if (manual) mutable.update { it.copy(status = when {
                    installed == null -> R.string.update_check_failed
                    release == null -> R.string.update_no_apk
                    else -> R.string.update_current
                }) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                failure(e)
                if (manual) mutable.update { it.copy(status = when {
                    e is GitHubFailure && e.code in listOf(403, 429) -> R.string.update_rate_limit
                    e is GitHubFailure && e.code == 404 -> R.string.update_no_public_release
                    else -> R.string.update_check_failed
                }) }
            } finally { mutable.update { it.copy(checking = false) } }
        }
    }

    private fun failure(error: Exception) {
        if (error is GitHubFailure && error.code in listOf(403, 429)) {
            prefs.edit().putLong("blocked-until", maxOf(error.retryAt, System.currentTimeMillis() + 60 * 60 * 1000)).apply()
        }
    }

    private suspend fun loadCurrentNotes() {
        if (!state.value.automatic || state.value.notes != null || System.currentTimeMillis() < prefs.getLong("blocked-until", 0)) return
        for (tag in listOf("v${BuildConfig.VERSION_NAME}", BuildConfig.VERSION_NAME)) {
            try {
                val json = api.release("tags/$tag")
                if (SemVer.parse(json.optString("tag_name")) != installed) return
                val notes = json.optString("body").takeIf { it.isNotBlank() } ?: return
                prefs.edit().putString("notes-version", tag).putString("notes", notes).apply()
                mutable.update { it.copy(notes = notes) }
                return
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                failure(e)
                if (e !is GitHubFailure || e.code != 404) return
            }
        }
    }

    fun closeWhatsNew() {
        prefs.edit().putInt("last-seen-code", BuildConfig.VERSION_CODE).apply()
        mutable.update { it.copy(whatsNew = false) }
        check(false)
    }

    fun markWhatsNewSeen() { prefs.edit().putInt("last-seen-code", BuildConfig.VERSION_CODE).apply() }

    fun later() {
        downloadJob?.cancel()
        api.cancelDownload()
        mutable.update { it.copy(offer = null, downloading = false, downloadError = null) }
    }

    fun update() {
        val release = state.value.offer ?: return
        if (state.value.downloading) return
        clearPending()
        mutable.update { it.copy(downloading = true, progress = 0f, downloadError = null, status = null) }
        downloadJob = viewModelScope.launch {
            try {
                val apk = api.download(release, folder) { fraction -> mutable.update { it.copy(progress = fraction) } }
                withContext(Dispatchers.IO) { validateUpdateApk(getApplication(), apk, release.version) }
                prefs.edit().putString("pending-version", release.tag).putBoolean("pending-file", true).apply()
                mutable.update { it.copy(downloading = false, offer = null, installReady = true) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                clearPending()
                mutable.update { it.copy(downloading = false, downloadError = R.string.update_download_failed) }
            }
        }
    }

    fun pendingFile(): File? = File(folder, "update.apk").takeIf { prefs.getBoolean("pending-file", false) && it.isFile }
    val awaitingPermission: Boolean get() = prefs.getBoolean("awaiting-permission", false)
    fun awaitPermission() { prefs.edit().putBoolean("awaiting-permission", true).apply() }
    fun resumePermission() {
        if (pendingFile() != null) mutable.update { it.copy(installReady = true) }
        else installFailed()
    }
    fun consumeInstall(): File? {
        if (!state.value.installReady) return null
        mutable.update { it.copy(installReady = false) }
        return pendingFile()
    }
    fun installerOpened() { prefs.edit().putBoolean("awaiting-permission", false).apply() }
    fun installFailed(permission: Boolean = false) {
        installerOpened()
        mutable.update { it.copy(status = if (permission) R.string.update_permission_denied else R.string.update_install_failed) }
    }
    private fun clearPending() {
        File(folder, "update.apk").delete()
        prefs.edit().remove("pending-version").remove("pending-file").remove("awaiting-permission").apply()
    }
}
