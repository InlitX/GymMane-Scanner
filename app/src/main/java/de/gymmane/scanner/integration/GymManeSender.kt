package de.gymmane.scanner.integration

import android.content.Context
import android.content.Intent
import de.gymmane.scanner.i18n.T

data class SendResult(val shared: Boolean, val error: String? = null)

/** Shares the unchanged import payload. Never reads or writes the clipboard. */
class GymManeSender(private val context: Context) {
    fun shareIntent(json: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, json)
    }

    fun send(json: String): SendResult = try {
        context.startActivity(Intent.createChooser(shareIntent(json), T.text("Mit GymMane teilen"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        SendResult(true)
    } catch (_: android.content.ActivityNotFoundException) {
        SendResult(false, T.text("Das Lebensmittel konnte nicht geteilt werden. Bitte erneut versuchen."))
    } catch (_: SecurityException) {
        SendResult(false, T.text("Das Lebensmittel konnte nicht geteilt werden. Bitte erneut versuchen."))
    }
}
