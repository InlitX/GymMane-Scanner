package de.gymmane.scanner.domain

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/** Reads EAN/UPC codes from a grayscale frame with ZXing, fully on device. */
class BarcodeDecoder {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN_8, BarcodeFormat.EAN_13, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E),
            DecodeHintType.TRY_HARDER to true,
        ))
    }

    /**
     * [luminance] holds [width] x [height] bytes, one per pixel, row by row.
     * Camera frames come in sensor orientation, so the frame is also tried turned by 90 degrees.
     */
    fun decode(luminance: ByteArray, width: Int, height: Int, rotationDegrees: Int = 0): String? {
        val upright = rotationDegrees == 90 || rotationDegrees == 270
        val first = if (upright) rotate(luminance, width, height) else Frame(luminance, width, height)
        val second = if (upright) Frame(luminance, width, height) else rotate(luminance, width, height)
        return read(first) ?: read(second)
    }

    private fun read(frame: Frame): String? = try {
        val source = PlanarYUVLuminanceSource(frame.data, frame.width, frame.height, 0, 0, frame.width, frame.height, false)
        val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        if (result.barcodeFormat == BarcodeFormat.UPC_E) Barcode.expandUpcE(result.text) else Barcode.normalize(result.text)
    } catch (_: ReaderException) {
        null
    } finally {
        reader.reset()
    }

    private class Frame(val data: ByteArray, val width: Int, val height: Int)

    private fun rotate(data: ByteArray, width: Int, height: Int): Frame {
        val out = ByteArray(data.size)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) out[x * height + (height - 1 - y)] = data[row + x]
        }
        return Frame(out, height, width)
    }
}
