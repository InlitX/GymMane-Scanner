package de.gymmane.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import de.gymmane.scanner.domain.BarcodeDecoder
import org.junit.Assert.assertEquals
import org.junit.Test

/** Test-only encoded images exercise the on-device ZXing recognizer. */
class BarcodeRecognitionTest {
    @Test fun recognizesEan8Ean13UpcAAndUpcEWithoutNetwork() {
        val decoder = BarcodeDecoder()
        listOf(
            Triple(BarcodeFormat.EAN_8, "96385074", "96385074"),
            Triple(BarcodeFormat.EAN_13, "4006381333931", "4006381333931"),
            Triple(BarcodeFormat.UPC_A, "036000291452", "0036000291452"),
            Triple(BarcodeFormat.UPC_E, "04252614", "0042100005264"),
        ).forEach { (format, value, expected) ->
            val matrix = MultiFormatWriter().encode(value, format, 1000, 500, mapOf(EncodeHintType.MARGIN to 12))
            val luminance = ByteArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) 0 else 255.toByte()
            }
            assertEquals("Recognition failed for $format", expected, decoder.decode(luminance, matrix.width, matrix.height))
        }
    }
}
