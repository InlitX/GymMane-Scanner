package de.gymmane.scanner

import androidx.test.platform.app.InstrumentationRegistry
import de.gymmane.scanner.data.ProductCache
import de.gymmane.scanner.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CacheTest {
    @Test fun entriesSurviveReopeningAndMissingValuesRemainUnknown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = "cache-test-${UUID.randomUUID()}.db"
        val fixture = Product("test", name = "Testeintrag", per100 = Nutrients(kcal = 0.0, sodium = .012), manual = true)
        try {
            ProductCache(context, database).use { it.save(fixture) }
            ProductCache(context, database).use { cache ->
                assertEquals(fixture, cache.find("test"))
                assertNull(cache.find("test")!!.per100.protein)
                cache.delete("test")
                assertTrue(cache.history().isEmpty())
            }
        } finally { context.deleteDatabase(database) }
    }

    @Test fun cacheBoundsScansButKeepsManualEntries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = "cache-test-${UUID.randomUUID()}.db"
        try {
            ProductCache(context, database).use { cache ->
                cache.save(Product("manual", name = "Manueller Test", manual = true))
                repeat(105) { index -> cache.save(Product("scan-$index", name = "Test $index")) }
                assertEquals(101, cache.history().size)
                assertNotNull(cache.find("manual"))
            }
        } finally { context.deleteDatabase(database) }
    }
}
