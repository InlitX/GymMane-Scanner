package de.gymmane.scanner.data

import de.gymmane.scanner.domain.Product
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProductServiceException(message: String) : IOException(message)

internal suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

class OpenFoodFactsApi(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = "https://world.openfoodfacts.org/".toHttpUrl(),
) {
    suspend fun product(barcode: String): Product {
        val url = baseUrl.newBuilder().addPathSegments("api/v2/product").addPathSegment("$barcode.json")
            .addQueryParameter("lc", "de")
            .addQueryParameter("fields", "code,product_name,product_name_de,brands,categories,categories_tags_de,categories_hierarchy,image_front_url,image_url,serving_size,serving_quantity,serving_quantity_unit,quantity,product_quantity,product_quantity_unit,packagings,nutrition_data_per,nutriments")
            .build()
        val request = Request.Builder().url(url)
            .header("User-Agent", "GymManeScanner/1.0.0 (Android; independent companion app)")
            .header("Accept", "application/json").build()
        return client.newCall(request).awaitResponse().use { response ->
            when {
                response.code == 404 -> throw ProductNotFound()
                response.code == 429 -> throw ProductServiceException("Zu viele Anfragen. Bitte warte eine Minute und versuche es erneut.")
                !response.isSuccessful -> throw ProductServiceException("Open Food Facts ist gerade nicht erreichbar (HTTP ${response.code}). Bitte später erneut versuchen.")
            }
            val body = response.body ?: throw InvalidProductResponse()
            if (body.contentLength() > 2_000_000) throw InvalidProductResponse()
            ProductJson.fromApi(body.string(), barcode)
        }
    }
}
