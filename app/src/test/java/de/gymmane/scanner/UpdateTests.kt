package de.gymmane.scanner

import de.gymmane.scanner.updates.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.security.MessageDigest

class UpdateTests {
    @get:Rule val temporary = TemporaryFolder()
    private fun v(value: String) = SemVer.parse(value)!!
    private fun asset(name: String = "app-universal.apk") = JSONObject().put("name", name).put("size", 4)
        .put("browser_download_url", "https://github.com/${UpdateConfig.REPOSITORY}/releases/download/v2.0.0/$name")
    private fun release(assets: JSONArray = JSONArray().put(asset())) = JSONObject().put("tag_name", "v2.0.0").put("body", "Release notes").put("assets", assets)

    @Test fun numericVersionsDoNotUseLexicographicOrder() {
        assertTrue(v("v1.10.0") > v("1.9.99"))
        assertTrue(v("2.0.0") > v("1.10000.0"))
        assertEquals(0, v("1.2.3+metadata").compareTo(v("v1.2.3+other")))
        assertTrue(v("999999999999999999999.0.0") > v("99.0.0"))
    }

    @Test fun prereleaseSemverOrderingMatchesTheSpecification() {
        val ordered = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0")
        ordered.zipWithNext().forEach { (a, b) -> assertTrue("$a < $b", v(a) < v(b)) }
    }

    @Test fun malformedVersionsAreRejected() {
        listOf("1.2", "1.02.3", "1.2.3-01", "latest", "1.2.3-", "1.2.3+", "1.2.3.4").forEach { assertNull(it, SemVer.parse(it)) }
    }

    @Test fun automaticChecksIncludeFailedAttemptsAndRespect24HoursAndRateLimits() {
        val now = 2 * UpdateConfig.INTERVAL_MS
        assertTrue(CheckSchedule.due(now, 0, 0))
        assertFalse(CheckSchedule.due(now, now - UpdateConfig.INTERVAL_MS + 1, 0))
        assertTrue(CheckSchedule.due(now, now - UpdateConfig.INTERVAL_MS, 0))
        assertFalse(CheckSchedule.due(now, 0, now + 1))
        assertFalse(CheckSchedule.due(now, now + 1000, 0))
    }

    @Test fun releasesWithoutApkDraftsAndPrereleasesAreIgnored() {
        assertNull(Release.parse(release(JSONArray().put(asset("source.zip"))), listOf("arm64-v8a")))
        assertNull(Release.parse(release().put("draft", true), listOf("arm64-v8a")))
        assertNull(Release.parse(release().put("prerelease", true), listOf("arm64-v8a")))
    }

    @Test fun selectsUniversalOrCompatibleAbiAndRetainsExactNotes() {
        val abis = JSONArray().put(asset("app-x86_64.apk")).put(asset("app-arm64-v8a.apk"))
        assertTrue(Release.parse(release(abis), listOf("arm64-v8a"))!!.assetUrl.endsWith("app-arm64-v8a.apk"))
        assertNull(Release.parse(release(JSONArray().put(asset("app-x86_64.apk"))), listOf("arm64-v8a")))
        assertEquals("Release notes", Release.parse(release(), listOf("arm64-v8a"))!!.notes)
    }

    @Test fun refusesAssetsOutsideConfiguredGithubRepository() {
        val external = asset().put("browser_download_url", "https://example.com/untrusted.apk")
        assertNull(Release.parse(release(JSONArray().put(external)), listOf("arm64-v8a")))
    }

    @Test fun latestRequestUsesPublicGithubEndpointWithoutCredentialsOrProductData() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(release().toString()))
            GitHubUpdates(OkHttpClient(), server.url("/").toString().trimEnd('/')).release()
            val request = server.takeRequest()
            assertEquals("/repos/${UpdateConfig.REPOSITORY}/releases/latest", request.path)
            assertNull(request.getHeader("Authorization")); assertNull(request.getHeader("Cookie"))
            assertTrue(request.body.size == 0L)
        }
    }

    @Test fun rateLimitHeadersAreReturnedForPersistedBackoff() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).addHeader("X-RateLimit-Reset", "2000000000"))
            try {
                GitHubUpdates(OkHttpClient(), server.url("/").toString().trimEnd('/')).release()
                fail("Expected rate limit")
            } catch (e: GitHubFailure) { assertEquals(429, e.code); assertEquals(2000000000000, e.retryAt) }
        }
    }

    @Test fun downloadsBoundedFileReportsProgressAndVerifiesHash() = runTest {
        MockWebServer().use { server ->
            val bytes = "test".toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            server.enqueue(MockResponse().setBody("test"))
            val model = Release("v2.0.0", v("2.0.0"), "notes", server.url("/asset.apk").toString(), 4, "sha256:$digest")
            var progress = 0f
            val file = GitHubUpdates().download(model, temporary.newFolder()) { progress = it }
            assertEquals("test", file.readText())
            assertEquals(1f, progress, 0f)
        }
    }

    @Test fun incompleteDownloadIsRejectedAndPartialFileRemoved() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("x"))
            val directory = temporary.newFolder()
            val model = Release("v2.0.0", v("2.0.0"), "", server.url("/asset.apk").toString(), 4, null)
            try { GitHubUpdates().download(model, directory) {}; fail("Expected truncated download") }
            catch (_: IOException) { }
            assertFalse(directory.resolve("update.apk.part").exists())
            assertFalse(directory.resolve("update.apk").exists())
        }
    }

    @Test fun incorrectDigestIsRejected() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("test"))
            val directory = temporary.newFolder()
            val model = Release("v2.0.0", v("2.0.0"), "", server.url("/asset.apk").toString(), 4, "sha256:" + "0".repeat(64))
            try { GitHubUpdates().download(model, directory) {}; fail("Expected invalid digest") }
            catch (_: IOException) { }
            assertFalse(directory.resolve("update.apk").exists())
        }
    }
}
