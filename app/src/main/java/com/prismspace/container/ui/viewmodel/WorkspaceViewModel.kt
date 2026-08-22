package com.prismspace.container.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prismspace.container.core.PrismEngineFacade
import com.prismspace.container.entity.pm.InstallResult
import com.prismspace.container.ui.PrismSpaceActivity
import com.prismspace.container.ui.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Sealed class representing mutually exclusive UI states for WorkspaceScreen.
 * Ensures only one state can be active at a time, eliminating rendering ambiguity.
 */
sealed class WorkspaceUiState {
    object Loading : WorkspaceUiState()
    object Empty : WorkspaceUiState()
    data class Success(val apps: List<AppItem>) : WorkspaceUiState()
    data class Error(val message: String) : WorkspaceUiState()
}

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private data class StartupPayload(
        val virtualizedApps: List<AppItem>,
        val installedApps: List<AppItem>
    )

    private val _uiState = MutableStateFlow<WorkspaceUiState>(WorkspaceUiState.Loading)
    val uiState: StateFlow<WorkspaceUiState> = _uiState

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredInstalledApps = MutableStateFlow<List<AppItem>>(emptyList())
    val filteredInstalledApps: StateFlow<List<AppItem>> = _filteredInstalledApps

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val virtualAppsChangeListener: () -> Unit = {
        viewModelScope.launch {
            refreshVirtualizedApps()
        }
    }

    init {
        PrismEngineFacade.addVirtualAppsChangedListener(virtualAppsChangeListener)
        refreshAllApps()
    }

    override fun onCleared() {
        PrismEngineFacade.removeVirtualAppsChangedListener(virtualAppsChangeListener)
        super.onCleared()
    }

    private suspend fun loadVirtualizedAppsInternal(): List<AppItem> {
        val virtualized = PrismEngineFacade.getVirtualizedApps()
        return convertToUiApps(virtualized, isInstalled = true, resolveRunningState = true)
    }

    private suspend fun ensureEngineReady(): Boolean {
        val appContext = getApplication<Application>().applicationContext
        return withContext(Dispatchers.IO) {
            PrismEngineFacade.initEngine(appContext)
        }
    }

    private suspend fun loadInstalledAppsInternal(): List<AppItem> {
        val installed = PrismEngineFacade.getInstalledApps()
        return convertToUiApps(installed, isInstalled = false, resolveRunningState = false).sortedBy { it.appName }
    }

    private fun applyInstalledApps(apps: List<AppItem>) {
        _installedApps.value = apps
        filterApps(_searchQuery.value)
    }

    private fun applyWorkspaceState(apps: List<AppItem>) {
        _uiState.value = if (apps.isEmpty()) {
            WorkspaceUiState.Empty
        } else {
            WorkspaceUiState.Success(apps)
        }
    }

    fun refreshAllApps() {
        viewModelScope.launch {
            _uiState.value = WorkspaceUiState.Loading
            try {
                val startupPayload = withContext(Dispatchers.IO) {
                    if (!ensureEngineReady()) {
                        return@withContext null
                    }
                    StartupPayload(
                        virtualizedApps = loadVirtualizedAppsInternal(),
                        installedApps = loadInstalledAppsInternal()
                    )
                }

                if (startupPayload == null) {
                    _uiState.value = WorkspaceUiState.Error("Engine initialization failed")
                    return@launch
                }

                applyInstalledApps(startupPayload.installedApps)
                applyWorkspaceState(startupPayload.virtualizedApps)
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Failed to load apps"
                _uiState.value = WorkspaceUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshVirtualizedApps() {
        viewModelScope.launch {
            _uiState.value = WorkspaceUiState.Loading
            try {
                if (!ensureEngineReady()) {
                    _uiState.value = WorkspaceUiState.Error("Engine initialization failed")
                    return@launch
                }
                applyWorkspaceState(loadVirtualizedAppsInternal())
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Failed to refresh apps"
                _uiState.value = WorkspaceUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            try {
                applyInstalledApps(loadInstalledAppsInternal())
            } catch (t: Throwable) {
                _toastMessage.value = t.message ?: "Failed to load device apps"
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps(query)
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            _installedApps.value
        } else {
            _installedApps.value.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        _filteredInstalledApps.value = filtered.sortedBy { it.appName }
    }

    suspend fun installApp(packageName: String): Boolean {
        return try {
            if (!ensureEngineReady()) {
                _toastMessage.value = "Engine not ready"
                return false
            }

            // Trigger immediate visible state via pending-clone bridge.
            refreshVirtualizedApps()
            val result: InstallResult = PrismEngineFacade.installApp(packageName)
            if (result.success) {
                applyWorkspaceState(loadVirtualizedAppsInternal())
                _toastMessage.value = "App added to Prism Space"
                true
            } else {
                applyWorkspaceState(loadVirtualizedAppsInternal())
                _toastMessage.value = result.msg ?: "Install failed"
                false
            }
        } catch (e: Exception) {
            _toastMessage.value = e.message ?: "Install failed"
            false
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val launched = withTimeout(30_000L) {
                    PrismEngineFacade.launchApp(packageName, 0)
                }
                if (!launched) {
                    _toastMessage.value = "Unable to launch app"
                }
            } catch (_: TimeoutCancellationException) {
                _toastMessage.value = "Launch timed out. Please try again."
            } catch (t: Throwable) {
                _toastMessage.value = t.message ?: "Unable to launch app"
            }
        }
    }

    fun clearClonedAppData(packageName: String, userId: Int = 0) {
        viewModelScope.launch {
            val cleared: Boolean = withContext(Dispatchers.IO) {
                PrismEngineFacade.clearAppData(packageName, userId)
            }
            _toastMessage.value = if (cleared) {
                "App data cleared"
            } else {
                "Failed to clear app data"
            }
        }
    }

    fun forceStopApp(packageName: String, userId: Int = 0) {
        viewModelScope.launch {
            val terminated: Boolean = withContext(Dispatchers.IO) {
                PrismEngineFacade.forceStopApp(packageName, userId)
            }
            _toastMessage.value = if (terminated) {
                "Process terminated"
            } else {
                "Unable to terminate process"
            }
            refreshVirtualizedApps()
        }
    }

    fun uninstallClonedApp(packageName: String, userId: Int = 0) {
        viewModelScope.launch {
            val uninstalled: Boolean = withContext(Dispatchers.IO) {
                PrismEngineFacade.uninstallApp(packageName, userId)
            }
            if (uninstalled) {
                _toastMessage.value = "App uninstalled"
                refreshVirtualizedApps()
            } else {
                _toastMessage.value = "Failed to uninstall app"
            }
        }
    }

    fun createShortcut(packageName: String, userId: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    _toastMessage.value = "Pinned shortcuts not supported on this Android version"
                    return@launch
                }

                val appContext = getApplication<Application>()
                val shortcutManager = appContext.getSystemService(ShortcutManager::class.java)
                if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) {
                    _toastMessage.value = "Launcher does not support pinned shortcuts"
                    return@launch
                }

                val appName = resolveAppName(packageName)
                val iconBitmap = PrismEngineFacade.getVirtualizedAppIconBitmap(packageName, userId)
                val launchIntent = Intent(appContext, PrismSpaceActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("shortcut_package_name", packageName)
                    putExtra("shortcut_user_id", userId)
                }

                val shortcut = ShortcutInfo.Builder(appContext, "prism_${packageName}_$userId")
                    .setShortLabel(appName)
                    .setLongLabel("Launch $appName in Prism Space")
                    .setIcon(
                        if (iconBitmap != null) {
                            Icon.createWithBitmap(iconBitmap)
                        } else {
                            Icon.createWithResource(appContext, appContext.applicationInfo.icon)
                        }
                    )
                    .setIntent(launchIntent)
                    .build()

                val requested = shortcutManager.requestPinShortcut(shortcut, null)
                _toastMessage.value = if (requested) {
                    "Shortcut request sent"
                } else {
                    "Unable to create shortcut"
                }
            } catch (t: Throwable) {
                _toastMessage.value = t.message ?: "Failed to create shortcut"
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun resolveAppName(packageName: String): String {
        val workspaceName = (uiState.value as? WorkspaceUiState.Success)
            ?.apps
            ?.firstOrNull { it.packageName == packageName }
            ?.appName
        if (!workspaceName.isNullOrBlank()) return workspaceName

        val installedName = installedApps.value
            .firstOrNull { it.packageName == packageName }
            ?.appName
        if (!installedName.isNullOrBlank()) return installedName

        return packageName.substringAfterLast('.')
    }

    private suspend fun convertToUiApps(
        apps: List<ApplicationInfo>,
        isInstalled: Boolean,
        resolveRunningState: Boolean
    ): List<AppItem> {
        return withContext(Dispatchers.IO) {
            val packageManager = getApplication<Application>().packageManager
            apps.mapNotNull { info ->
                try {
                    val resolvedName = info.loadLabel(packageManager).toString()
                    val icon = info.loadIcon(packageManager)
                    val isRunning = if (resolveRunningState) {
                        PrismEngineFacade.isAppRunning(info.packageName, 0)
                    } else {
                        false
                    }
                    AppItem(
                        packageName = info.packageName,
                        appName = resolvedName,
                        icon = null,
                        iconDrawable = icon,
                        isInstalled = isInstalled,
                        isRunning = isRunning
                    )
                } catch (_: Exception) {
                    // Silently skip apps that fail to load
                    null
                }
            }
        }
    }
}


