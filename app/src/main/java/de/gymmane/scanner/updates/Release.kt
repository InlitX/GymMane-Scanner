package de.gymmane.scanner.updates

import java.math.BigInteger
import org.json.JSONObject

/** SemVer 2.0 precedence; build metadata does not affect ordering. */
data class SemVer(val major: BigInteger, val minor: BigInteger, val patch: BigInteger, val pre: List<String>) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        for ((a, b) in listOf(major to other.major, minor to other.minor, patch to other.patch)) {
            if (a != b) return a.compareTo(b)
        }
        if (pre.isEmpty() || other.pre.isEmpty()) return when {
            pre.isEmpty() && other.pre.isEmpty() -> 0
            pre.isEmpty() -> 1
            else -> -1
        }
        for (i in 0 until minOf(pre.size, other.pre.size)) {
            val a = pre[i]; val b = other.pre[i]
            val an = a.toBigIntegerOrNull(); val bn = b.toBigIntegerOrNull()
            val c = when {
                an != null && bn != null -> an.compareTo(bn)
                an != null -> -1
                bn != null -> 1
                else -> a.compareTo(b)
            }
            if (c != 0) return c
        }
        return pre.size.compareTo(other.pre.size)
    }
    companion object {
        fun parse(value: String): SemVer? {
            val m = Regex("^[vV]?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$").matchEntire(value.trim()) ?: return null
            val pre = m.groupValues[4].takeIf(String::isNotEmpty)?.split('.') ?: emptyList()
            if (pre.any { it.all(Char::isDigit) && it.length > 1 && it.startsWith('0') }) return null
            return SemVer(m.groupValues[1].toBigInteger(), m.groupValues[2].toBigInteger(), m.groupValues[3].toBigInteger(), pre)
        }
    }
}

object UpdateConfig {
    // The only OWNER/REPO configuration for API requests and asset URL validation.
    const val REPOSITORY = "KingMezzo/GymMane-Scanner"
    const val INTERVAL_MS = 24 * 60 * 60 * 1000L
}

data class Release(val tag: String, val version: SemVer, val notes: String, val assetUrl: String, val size: Long, val digest: String?) {
    companion object {
        fun parse(json: JSONObject, supportedAbis: List<String>): Release? {
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null
            val tag = json.optString("tag_name")
            val version = SemVer.parse(tag) ?: return null
            val assets = json.optJSONArray("assets") ?: return null
            val architectures = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            val candidates = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }.filter {
                it.optString("name").endsWith(".apk", true) && it.optString("browser_download_url")
                    .startsWith("https://github.com/${UpdateConfig.REPOSITORY}/releases/download/", ignoreCase = true) && it.optLong("size") in 1..536870912
            }
            fun rank(asset: JSONObject): Int {
                val name = asset.optString("name").lowercase()
                if ("universal" in name || "all-abis" in name) return 0
                val abi = architectures.firstOrNull { it in name } ?: return 1
                val index = supportedAbis.indexOf(abi)
                return if (index >= 0) index + 2 else Int.MAX_VALUE
            }
            val asset = candidates.filter { rank(it) < Int.MAX_VALUE }.minByOrNull(::rank) ?: return null
            return Release(tag, version, json.optString("body", ""), asset.optString("browser_download_url"),
                asset.optLong("size"), asset.optString("digest").takeIf { it.matches(Regex("sha256:[0-9a-fA-F]{64}")) })
        }
    }
}

object CheckSchedule {
    fun due(now: Long, lastAttempt: Long, blockedUntil: Long): Boolean =
        now >= blockedUntil && (lastAttempt == 0L || now - lastAttempt >= UpdateConfig.INTERVAL_MS)
}
