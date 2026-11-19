package de.gymmane.scanner.data

import de.gymmane.scanner.domain.Product

interface ProductStore {
    suspend fun history(): List<Product>
    suspend fun lookup(barcode: String, refresh: Boolean = false): ProductResult
    suspend fun save(product: Product)
    suspend fun delete(product: Product)
}
