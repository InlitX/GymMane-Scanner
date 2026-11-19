package de.gymmane.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import de.gymmane.scanner.ui.ScannerApp
import de.gymmane.scanner.ui.ScannerViewModel
import de.gymmane.scanner.ui.theme.GymManeTheme

class MainActivity : ComponentActivity() {
    private val updates by viewModels<de.gymmane.scanner.updates.UpdateViewModel>()
    override fun attachBaseContext(newBase: android.content.Context) {
        val language = de.gymmane.scanner.data.ScannerSettings(newBase).language
        val locale = java.util.Locale.forLanguageTag(language)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
        de.gymmane.scanner.i18n.T.configure(newBase, language)
    }
    private val viewModel by viewModels<ScannerViewModel> {
        val app = application as ScannerApplication
        ScannerViewModel.Factory(app.repository, app.settings)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { GymManeTheme { ScannerApp(viewModel, updates) } }
    }
    override fun onStart() {
        super.onStart()
        updates.check(false)
    }
}
