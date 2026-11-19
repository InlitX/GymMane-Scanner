package de.gymmane.scanner

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import de.gymmane.scanner.updates.apkInstallIntent
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UpdateInstallerTest {
    @Test fun installerReceivesOnlyContentUriWithReadGrant() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(directory, "provider-test.apk")
        try {
            file.writeText("test-only URI fixture")
            val intent = apkInstallIntent(context, file)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("application/vnd.android.package-archive", intent.type)
            assertEquals("content", intent.data!!.scheme)
            assertEquals("${context.packageName}.updates", intent.data!!.authority)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
        } finally { file.delete() }
    }

    @Test(expected = IllegalArgumentException::class)
    fun providerDoesNotExposeOtherCacheDirectories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        apkInstallIntent(context, File(context.cacheDir, "outside-update-cache.txt"))
    }
}
