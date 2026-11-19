package de.gymmane.scanner

import android.app.Application
import de.gymmane.scanner.data.ProductRepository
import de.gymmane.scanner.data.ScannerSettings

class ScannerApplication : Application() {
    val repository by lazy { ProductRepository(this) }
    val settings by lazy { ScannerSettings(this) }
}
