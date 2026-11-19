package de.gymmane.scanner.domain

object Barcode {
    fun isValid(value: String): Boolean {
        if (value.length !in listOf(8, 12, 13) || value.any { it !in '0'..'9' }) return false
        val sum = value.dropLast(1).reversed().mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 3 else 1 }.sum()
        return (10 - sum % 10) % 10 == value.last() - '0'
    }

    fun normalize(value: String): String? {
        val clean = value.trim()
        if (!isValid(clean)) return null
        return if (clean.length == 12) "0$clean" else clean
    }

    /** UPC-E is distinguishable from EAN-8 only through ML Kit's format. */
    fun expandUpcE(value: String): String? {
        if (value.length != 8 || value.any { it !in '0'..'9' } || value.first() !in "01") return null
        val d = value.substring(1, 7)
        val body = when (d[5]) {
            '0', '1', '2' -> "${value[0]}${d.take(2)}${d[5]}0000${d.substring(2, 5)}"
            '3' -> "${value[0]}${d.take(3)}00000${d.substring(3, 5)}"
            '4' -> "${value[0]}${d.take(4)}00000${d[4]}"
            else -> "${value[0]}${d.take(5)}0000${d[5]}"
        }
        return normalize(body + value.last())
    }
}
