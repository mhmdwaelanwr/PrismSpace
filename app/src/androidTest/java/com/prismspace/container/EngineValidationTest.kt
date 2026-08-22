package com.prismspace.container

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prismspace.container.core.EngineCapabilities
import com.prismspace.container.core.NativeCore
import com.prismspace.container.core.PrismEngineFacade
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EngineValidationTest {

    @Test
    fun runtimeTruth_isObservableAndWritesReport() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext.applicationContext
        val capabilities = EngineCapabilities.get()

        val initOk = PrismEngineFacade.initEngine(appContext)
        assertTrue("Engine init failed", initOk)

        PrismEngineFacade.refreshDiagnostics(appContext)
        awaitNativeBootstrapTruth(capabilities)

        val report = buildReport(capabilities)
        writeReport(appContext.filesDir, report)
        Log.i(TAG, "$REPORT_BEGIN\n$report$REPORT_END")

        val pageSize = capabilities.deviceFacts()["pageSize"]?.toLongOrNull() ?: -1L
        assertTrue("Invalid runtime page size: $pageSize\n$report", pageSize > 0L)

        assertObservableContract(
            capabilities,
            EngineCapabilities.Component.CORE_SERVICES,
            allowDegraded = true,
            report = report
        )
        assertObservableContract(
            capabilities,
            EngineCapabilities.Component.NATIVE_BOOTSTRAP,
            allowDegraded = true,
            report = report
        )
    }

    private suspend fun awaitNativeBootstrapTruth(capabilities: EngineCapabilities) {
        val deadline = SystemClock.elapsedRealtime() + NATIVE_TRUTH_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            NativeCore.refreshCapabilities()
            val state = capabilities
                .getStatus(EngineCapabilities.Component.NATIVE_BOOTSTRAP)
                .state
            if (state != EngineCapabilities.State.UNKNOWN) return
            delay(NATIVE_TRUTH_POLL_MS)
        }
    }

    private fun assertObservableContract(
        capabilities: EngineCapabilities,
        component: EngineCapabilities.Component,
        allowDegraded: Boolean,
        report: String
    ) {
        val status = capabilities.getStatus(component)
        val allowed = if (allowDegraded) {
            status.state == EngineCapabilities.State.ACTIVE ||
                status.state == EngineCapabilities.State.DEGRADED
        } else {
            status.state == EngineCapabilities.State.ACTIVE
        }
        assertTrue(
            "$component did not produce acceptable runtime evidence: ${status.state} (${status.detail})\n$report",
            allowed
        )
    }

    private fun buildReport(capabilities: EngineCapabilities): String = buildString {
        appendLine("validationSchema=1")
        appendLine("timestampMillis=${System.currentTimeMillis()}")
        appendLine("nativeReady=${NativeCore.isReady()}")
        appendLine("--- engine truth ---")
        append(capabilities.toDiagnosticText())
    }

    private fun writeReport(filesDir: File, report: String) {
        val diagnosticsDir = File(filesDir, "diagnostics")
        assertTrue("Unable to create diagnostics dir: $diagnosticsDir", diagnosticsDir.exists() || diagnosticsDir.mkdirs())

        val latest = File(diagnosticsDir, "latest.txt")
        latest.writeText(report)

        val sdk = EngineCapabilities.get().deviceFacts()["sdk"] ?: "unknown"
        val timestamp = System.currentTimeMillis()
        File(diagnosticsDir, "engine-validation-sdk${sdk}-$timestamp.txt").writeText(report)
    }

    companion object {
        private const val TAG = "PrismDeviceValidation"
        private const val REPORT_BEGIN = "PRISM_DEVICE_VALIDATION_REPORT_BEGIN"
        private const val REPORT_END = "PRISM_DEVICE_VALIDATION_REPORT_END"
        private const val NATIVE_TRUTH_TIMEOUT_MS = 5_000L
        private const val NATIVE_TRUTH_POLL_MS = 100L
    }
}
