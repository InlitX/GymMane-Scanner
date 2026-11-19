package de.gymmane.scanner

import androidx.lifecycle.viewModelScope
import de.gymmane.scanner.data.*
import de.gymmane.scanner.domain.*
import de.gymmane.scanner.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SendFlowTest {
    private val dispatcher = StandardTestDispatcher()
    private val valid = Product("4006381333931", barcode = "4006381333931", name = "Testlebensmittel",
        per100 = Nutrients(100.0, 1.0, 10.0, 3.0))
    private val prefs = object : AutoSendSettings { override var autoSend = false }
    private var result = valid
    private val store = object : ProductStore {
        override suspend fun history() = listOf(result)
        override suspend fun lookup(barcode: String, refresh: Boolean) = ProductResult(result, cached = true)
        override suspend fun save(product: Product) { result = product }
        override suspend fun delete(product: Product) = Unit
    }
    private lateinit var vm: ScannerViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); vm = ScannerViewModel(store, prefs) }
    @After fun cleanup() { vm.viewModelScope.cancel(); Dispatchers.resetMain() }

    @Test fun `default off scan does not queue a handoff`() = runTest(dispatcher) {
        vm.lookup(valid.id); advanceUntilIdle()
        assertEquals(Screen.PRODUCT, vm.state.value.screen)
        assertNull(vm.state.value.pendingSend)
    }

    @Test fun `auto on scan queues one handoff even for a cached product`() = runTest(dispatcher) {
        vm.setAutoSend(true); vm.lookup(valid.id); advanceUntilIdle()
        val event = vm.state.value.pendingSend!!
        assertTrue(prefs.autoSend)
        assertNotNull(vm.consumeSend(event.id))
        assertNull(vm.consumeSend(event.id))
        assertNull(vm.state.value.pendingSend)
    }

    @Test fun `opening history and refreshing never auto sends`() = runTest(dispatcher) {
        vm.setAutoSend(true); vm.open(valid); advanceUntilIdle()
        assertNull(vm.state.value.pendingSend)
        vm.lookup(valid.id, refresh = true); advanceUntilIdle()
        assertNull(vm.state.value.pendingSend)
    }

    @Test fun `manual send works with auto off and leaving cancels unconsumed handoff`() = runTest(dispatcher) {
        vm.send(valid)
        assertNotNull(vm.state.value.pendingSend)
        vm.navigate(Screen.HISTORY)
        assertNull(vm.state.value.pendingSend)
    }

    @Test fun `missing data blocks clipboard handoff until user edits and saves`() = runTest(dispatcher) {
        result = valid.copy(per100 = valid.per100.copy(protein = null))
        vm.setAutoSend(true); vm.lookup(valid.id); advanceUntilIdle()
        assertNull(vm.state.value.pendingSend)
        assertNotNull(vm.state.value.correction)
        assertTrue(vm.state.value.correctionError!!.contains("Eiweiß"))
        vm.editForSending()
        assertEquals(Screen.MANUAL, vm.state.value.screen)
        assertTrue(vm.state.value.editingForSend)
        vm.save(valid.copy(manual = true)); advanceUntilIdle()
        assertNotNull(vm.state.value.pendingSend)
        assertEquals(Screen.PRODUCT, vm.state.value.screen)
    }

    @Test fun `canceling correction never sends or changes product`() = runTest(dispatcher) {
        vm.send(valid.copy(name = "")); vm.dismissCorrection(); advanceUntilIdle()
        assertNull(vm.state.value.pendingSend)
        assertNull(vm.state.value.correction)
        assertEquals(valid, result)
    }

    @Test fun `saved preference is used by a new viewmodel without sending on startup`() = runTest(dispatcher) {
        vm.setAutoSend(true)
        vm.viewModelScope.cancel()
        vm = ScannerViewModel(store, prefs)
        advanceUntilIdle()
        assertTrue(vm.state.value.autoSend)
        assertNull(vm.state.value.pendingSend)
    }
}
