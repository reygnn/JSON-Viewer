package com.github.reygnn.json_viewer.ui.viewer

import android.net.Uri
import app.cash.turbine.test
import com.github.reygnn.json_viewer.MainDispatcherRule
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.util.Resource
import com.github.reygnn.json_viewer.domain.model.JsonData
import com.github.reygnn.json_viewer.domain.model.JsonLine
import com.github.reygnn.json_viewer.domain.usecase.LoadJsonUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class JsonViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loadJsonUseCase: LoadJsonUseCase = mockk()

    // One dispatcher everywhere: the rule's test dispatcher (see TESTING_CONVENTIONS).
    private val dispatchers = object : CoroutineDispatchers {
        override val main = mainDispatcherRule.dispatcher
        override val io = mainDispatcherRule.dispatcher
        override val default = mainDispatcherRule.dispatcher
    }

    private val uri: Uri = mockk()

    private val sampleData = JsonData(
        fileName = "test.json",
        fileSize = 1234L,
        jsonLines = listOf(JsonLine(0, "{"), JsonLine(1, """  "a": 1"""), JsonLine(2, "}")),
    )

    private fun viewModel() = JsonViewerViewModel(loadJsonUseCase, dispatchers)

    @Test
    fun `successful load exposes jsonData and clears loading`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { loadJsonUseCase(uri) } returns Resource.Success(sampleData)

        val vm = viewModel()
        vm.loadJsonFromUri(uri)

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.jsonData).isEqualTo(sampleData)
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `error result surfaces message and clears loading`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { loadJsonUseCase(uri) } returns Resource.Error("boom")

        val vm = viewModel()
        vm.loadJsonFromUri(uri)

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.jsonData).isNull()
        assertThat(state.errorMessage).isEqualTo("boom")
    }

    @Test
    fun `onErrorShown clears the error message`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { loadJsonUseCase(uri) } returns Resource.Error("boom")

        val vm = viewModel()
        vm.loadJsonFromUri(uri)
        vm.onErrorShown()

        assertThat(vm.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `state ends on the loaded data when observed via flow`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { loadJsonUseCase(uri) } returns Resource.Success(sampleData)

        val vm = viewModel()
        vm.uiState.test {
            assertThat(awaitItem().jsonData).isNull()   // initial empty state
            vm.loadJsonFromUri(uri)
            val latest = expectMostRecentItem()
            assertThat(latest.isLoading).isFalse()
            assertThat(latest.jsonData).isEqualTo(sampleData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unexpected exception is caught and surfaced as critical error`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { loadJsonUseCase(uri) } throws IllegalStateException("kapow")

        val vm = viewModel()
        vm.loadJsonFromUri(uri)

        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).contains("kapow")
    }
}
