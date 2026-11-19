package de.gymmane.scanner

import de.gymmane.scanner.data.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.After
import org.junit.Test

class ApiTests {
    private val server = MockWebServer()
    private val api = OpenFoodFactsApi(OkHttpClient(), server.url("/"))
    @After fun close() { server.shutdown() }

    @Test fun `lookup requests only selected fields with identifiable user agent`() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":1,"product":{"product_name_de":"Testdaten","nutriments":{"proteins_100g":10}}}"""))
        val product = api.product("4006381333931")
        assertEquals("Testdaten", product.name)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/v2/product/4006381333931.json?"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("GymManeScanner/"))
        assertEquals("de", request.requestUrl!!.queryParameter("lc"))
        val fields = request.requestUrl!!.queryParameter("fields")!!.split(',')
        assertTrue(fields.containsAll(listOf("serving_size", "serving_quantity", "quantity", "product_quantity", "packagings")))
    }

    @Test fun `HTTP 404 becomes product not found`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        try { api.product("123"); fail("Expected not found") } catch (_: ProductNotFound) { }
    }

    @Test fun `rate limiting offers a specific retry message`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        try { api.product("123"); fail("Expected rate limit") }
        catch (e: ProductServiceException) { assertTrue(e.message!!.contains("Minute")) }
    }

    @Test fun `service failure is not shown as an unknown product`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try { api.product("123"); fail("Expected service error") }
        catch (e: ProductServiceException) { assertTrue(e.message!!.contains("503")) }
    }
}
