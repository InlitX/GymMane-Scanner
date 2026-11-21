package de.gymmane.scanner.data

import android.content.Context
import de.gymmane.scanner.domain.Product
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class ProductResult(val product: Product, val cached: Boolean)

class ProductRepository(context: Context) : ProductStore {
    private val cache = ProductCache(context.applicationContext)
    private val mutex = Mutex()
    private val imageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val images = File(context.filesDir, "product-images").apply { mkdirs() }
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()
    private val imageClient = client.newBuilder().callTimeout(5, TimeUnit.SECONDS).build()
    private val settings = ScannerSettings(context.applicationContext)
    private val api = OpenFoodFactsApi(client, language = { settings.language })

    override suspend fun history(): List<Product> = withContext(Dispatchers.IO) { mutex.withLock { cache.history() } }

    override suspend fun lookup(barcode: String, refresh: Boolean): ProductResult = withContext(Dispatchers.IO) {
        val saved = mutex.withLock { cache.find(barcode) }
        if (saved != null && (!refresh || saved.manual)) {
            mutex.withLock { cache.save(saved) }
            return@withContext ProductResult(saved, true)
        }
        val product = api.product(barcode)
        val stored = product.copy(localImage = saved?.takeIf { it.imageUrl == product.imageUrl }?.localImage?.takeIf { File(it).exists() })
        save(stored)
        imageScope.launch {
          try {
            val image = downloadImage(product) ?: return@launch
            mutex.withLock {
                val current = cache.find(product.id)
                // Do not resurrect a deleted product or overwrite a newer edit.
                if (current != null && current.fetchedAt == product.fetchedAt && current.imageUrl == product.imageUrl) {
                    cache.save(current.copy(localImage = image))
                }
                pruneImages()
            }
          } catch (e: CancellationException) { throw e }
          catch (_: Exception) { /* An optional image must never fail the product lookup. */ }
        }
        ProductResult(stored, false)
    }

    override suspend fun save(product: Product) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val previous = cache.find(product.id)
            val merged = if (product.localImage == null && previous?.fetchedAt == product.fetchedAt && previous.imageUrl == product.imageUrl)
                product.copy(localImage = previous.localImage) else product
            cache.save(merged)
            pruneImages()
        }
    }

    override suspend fun delete(product: Product) = withContext(Dispatchers.IO) {
        mutex.withLock { cache.delete(product.id); pruneImages() }
    }

    private fun pruneImages() {
        val kept = cache.history().mapNotNull { it.localImage }.toSet()
        images.listFiles()?.filter { it.extension != "download" && it.absolutePath !in kept }?.forEach { it.delete() }
    }

    private suspend fun downloadImage(product: Product): String? {
        val url = product.imageUrl ?: return null
        val target = File(images, "${product.id}-${product.fetchedAt}.img")
        val temporary = File(images, "${product.id}-${product.fetchedAt}.download")
        return try {
            imageClient.newCall(Request.Builder().url(url)
                .header("User-Agent", "GymManeScanner/1.0.0 (Android)").build()).awaitResponse().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null || body.contentLength() > 3_000_000 ||
                    body.contentType()?.type != "image") return null
                body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > 3_000_000) throw java.io.IOException("Bild zu groß")
                            output.write(buffer, 0, count)
                        }
                    }
                }
                if (!temporary.renameTo(target)) throw java.io.IOException("Bild konnte nicht gespeichert werden")
                target.absolutePath
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        finally { temporary.delete() }
    }
}
