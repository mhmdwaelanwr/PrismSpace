package com.prismspace.container.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prismspace.container.core.PrismEngineFacade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing app-level UI state
 * Handles theme, navigation, and user preferences
 */
class PrismSpaceViewModel : ViewModel() {

    // ===== UI State =====
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _settingsActionMessage = MutableStateFlow<String?>(null)
    val settingsActionMessage: StateFlow<String?> = _settingsActionMessage

    // ===== User Preferences =====
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled

    private val _syncInterval = MutableStateFlow(15) // minutes
    val syncInterval: StateFlow<Int> = _syncInterval

    // ===== Statistics =====
    data class Statistics(
        val projectsCount: Int = 12,
        val tasksCount: Int = 48,
        val completionPercentage: Int = 89
    )

    private val _statistics = MutableStateFlow(Statistics())
    val statistics: StateFlow<Statistics> = _statistics

    // ===== Activity Items =====
    data class ActivityItem(
        val id: String,
        val title: String,
        val description: String,
        val timestamp: Long,
        val icon: String
    )

    private val _recentActivity = MutableStateFlow<List<ActivityItem>>(emptyList())
    val recentActivity: StateFlow<List<ActivityItem>> = _recentActivity

    // ===== UI Actions =====

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkMode.emit(enabled)
        }
    }

    fun selectTab(tabIndex: Int) {
        viewModelScope.launch {
            _currentTab.emit(tabIndex)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _notificationsEnabled.emit(enabled)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _autoSyncEnabled.emit(enabled)
        }
    }

    fun setSyncInterval(minutes: Int) {
        viewModelScope.launch {
            _syncInterval.emit(minutes)
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.emit(true)
            try {
                // Simulate loading delay
                kotlinx.coroutines.delay(800)
                _statistics.emit(Statistics(
                    projectsCount = 12,
                    tasksCount = 48,
                    completionPercentage = 89
                ))
                _error.emit(null)
            } catch (e: Exception) {
                _error.emit(e.message)
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    fun loadRecentActivity() {
        viewModelScope.launch {
            _isLoading.emit(true)
            try {
                // Simulate loading delay
                kotlinx.coroutines.delay(800)
                _recentActivity.emit(listOf(
                    ActivityItem(
                        id = "1",
                        title = "Workspace updated",
                        description = "Your workspace has been synchronized",
                        timestamp = System.currentTimeMillis() - 900000,
                        icon = "workspace"
                    ),
                    ActivityItem(
                        id = "2",
                        title = "New task added",
                        description = "A new task has been created",
                        timestamp = System.currentTimeMillis() - 3600000,
                        icon = "task"
                    ),
                    ActivityItem(
                        id = "3",
                        title = "File synchronized",
                        description = "All files have been synced",
                        timestamp = System.currentTimeMillis() - 10800000,
                        icon = "sync"
                    )
                ))
                _error.emit(null)
            } catch (e: Exception) {
                _error.emit(e.message)
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    fun clearError() {
        viewModelScope.launch {
            _error.emit(null)
        }
    }

    fun clearSettingsActionMessage() {
        viewModelScope.launch {
            _settingsActionMessage.emit(null)
        }
    }

    fun forceStopAllRunningApps(userId: Int = 0) {
        viewModelScope.launch {
            _isLoading.emit(true)
            _error.emit(null)
            try {
                val stoppedCount = PrismEngineFacade.stopAllRunningApps(userId)
                _settingsActionMessage.emit("Force-stopped $stoppedCount app(s)")
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to force-stop apps")
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    fun clearAllAppData(userId: Int = 0) {
        viewModelScope.launch {
            _isLoading.emit(true)
            _error.emit(null)
            try {
                val clearedCount = PrismEngineFacade.clearAllAppData(userId)
                _settingsActionMessage.emit("Cleared data for $clearedCount app(s)")
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to clear app data")
            } finally {
                _isLoading.emit(false)
            }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            _isDarkMode.emit(true)
            _notificationsEnabled.emit(true)
            _autoSyncEnabled.emit(true)
            _syncInterval.emit(15)
        }
    }
}

