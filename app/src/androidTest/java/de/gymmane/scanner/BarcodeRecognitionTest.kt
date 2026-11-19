package de.gymmane.scanner

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Test-only encoded images exercise the real bundled ML Kit recognizer. */
class BarcodeRecognitionTest {
    @Test fun recognizesEan8Ean13UpcAAndUpcEWithoutNetwork() {
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_8, Barcode.FORMAT_EAN_13, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).build())
        try {
            listOf(
                BarcodeFormat.EAN_8 to "96385074",
                BarcodeFormat.EAN_13 to "4006381333931",
                BarcodeFormat.UPC_A to "036000291452",
                BarcodeFormat.UPC_E to "04252614",
            ).forEach { (format, value) ->
                val matrix = MultiFormatWriter().encode(value, format, 1000, 500)
                val pixels = IntArray(matrix.width * matrix.height) { index -> if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
                val bitmap = Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                val result = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 20, TimeUnit.SECONDS)
                assertTrue("Recognition failed for $format", result.any {
                    it.rawValue == value || (format == BarcodeFormat.UPC_A && it.rawValue == "0$value")
                })
                bitmap.recycle()
            }
        } finally { scanner.close() }
    }
}
