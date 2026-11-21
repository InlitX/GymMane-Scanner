package de.gymmane.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import de.gymmane.scanner.domain.BarcodeDecoder
import org.junit.Assert.*
import org.junit.Test

class BarcodeDecoderTest {
    private val codes = listOf(
        BarcodeFormat.EAN_8 to "96385074",
        BarcodeFormat.EAN_13 to "4006381333931",
        BarcodeFormat.UPC_A to "036000291452",
        BarcodeFormat.UPC_E to "04252614",
    )

    private class Frame(val data: ByteArray, val width: Int, val height: Int)

    private fun frame(format: BarcodeFormat, value: String): Frame {
        val matrix = MultiFormatWriter().encode(value, format, 600, 240, mapOf(EncodeHintType.MARGIN to 12))
        val data = ByteArray(matrix.width * matrix.height) { i ->
            if (matrix[i % matrix.width, i / matrix.width]) 0 else 255.toByte()
        }
        return Frame(data, matrix.width, matrix.height)
    }

    private fun turned(f: Frame): Frame {
        val out = ByteArray(f.data.size)
        for (y in 0 until f.height) for (x in 0 until f.width) out[x * f.height + (f.height - 1 - y)] = f.data[y * f.width + x]
        return Frame(out, f.height, f.width)
    }

    private fun expected(format: BarcodeFormat, value: String) = when (format) {
        BarcodeFormat.UPC_A -> "0$value"
        BarcodeFormat.UPC_E -> "0042100005264"
        else -> value
    }

    @Test fun `reads EAN-8, EAN-13, UPC-A and UPC-E like the camera did before`() {
        val decoder = BarcodeDecoder()
        codes.forEach { (format, value) ->
            val f = frame(format, value)
            assertEquals("$format", expected(format, value), decoder.decode(f.data, f.width, f.height))
        }
    }

    @Test fun `reads a frame in sensor orientation, whatever the rotation says`() {
        val decoder = BarcodeDecoder()
        codes.forEach { (format, value) ->
            val f = turned(frame(format, value))
            assertEquals("$format at 90", expected(format, value), decoder.decode(f.data, f.width, f.height, 90))
            assertEquals("$format at 0", expected(format, value), decoder.decode(f.data, f.width, f.height, 0))
        }
    }

    @Test fun `an empty frame finds nothing`() {
        val blank = ByteArray(640 * 480) { 200.toByte() }
        assertNull(BarcodeDecoder().decode(blank, 640, 480, 90))
    }
}
