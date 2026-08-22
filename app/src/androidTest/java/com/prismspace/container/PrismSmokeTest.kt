package com.prismspace.container

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prismspace.container.core.PrismEngineFacade
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrismSmokeTest {

    @Test
    fun addListLaunch_smoke() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val engineReady = PrismEngineFacade.initEngine(appContext)
        assertTrue("Engine init failed", engineReady)

        val candidate = PrismEngineFacade.getInstalledApps()
            .asSequence()
            .map { it.packageName }
            .filter { it != appContext.packageName }
            .firstOrNull { pkg -> appContext.packageManager.getLaunchIntentForPackage(pkg) != null }
            ?: throw AssertionError("No launchable candidate package found for smoke test")

        val install = PrismEngineFacade.installApp(candidate, 0)
        assertTrue("installPackageAsUser failed for $candidate: ${install.msg}", install.success)

        val virtualized = PrismEngineFacade.getVirtualizedApps(0)
        assertTrue(
            "getVirtualizedApps does not contain $candidate",
            virtualized.any { it.packageName == candidate }
        )

        val launched = PrismEngineFacade.launchApp(candidate, 0)
        assertTrue("launchApk failed for $candidate", launched)
    }

    @Test
    fun addListLaunch_blockingInstall_survivesPolicyDegraded() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val engineReady = PrismEngineFacade.initEngine(appContext)
        assertTrue("Engine init failed", engineReady)

        val candidate = PrismEngineFacade.getInstalledApps()
            .asSequence()
            .map { it.packageName }
            .filter { it != appContext.packageName }
            .firstOrNull { pkg -> appContext.packageManager.getLaunchIntentForPackage(pkg) != null }
            ?: throw AssertionError("No launchable candidate package found for smoke test")

        val install = PrismEngineFacade.installAppBlocking(candidate, 0)
        assertTrue("installPackageAsUser failed for $candidate: ${install.msg}", install.success)

        // If policy publish degrades, install still must surface in list and remain launchable.
        val virtualized = PrismEngineFacade.getVirtualizedApps(0)
        assertTrue(
            "degraded policy path must not hide $candidate from getVirtualizedApps; msg=${install.msg}",
            virtualized.any { it.packageName == candidate }
        )

        val launched = PrismEngineFacade.launchApp(candidate, 0)
        assertTrue("launchApk failed for $candidate", launched)
    }
}
