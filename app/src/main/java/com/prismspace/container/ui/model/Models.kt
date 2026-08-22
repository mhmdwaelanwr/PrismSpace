package com.prismspace.container.ui.model

/**
 * Data models for UI state management
 */

// User Profile Model
data class UserProfile(
    val id: String = "",
    val name: String = "User",
    val email: String = "",
    val avatar: String? = null,
    val isPremium: Boolean = true,
    val joinDate: Long = System.currentTimeMillis()
)

// Project Model
data class Project(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val progress: Float = 0f,
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val category: String = "",
    val status: ProjectStatus = ProjectStatus.ACTIVE
)

enum class ProjectStatus {
    ACTIVE, COMPLETED, ARCHIVED, ON_HOLD
}

// Task Model
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val projectId: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueDate: Long? = null,
    val assignee: String = "",
    val tags: List<String> = emptyList(),
    val createdDate: Long = System.currentTimeMillis()
)

enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, REVIEW, COMPLETED, BLOCKED
}

// Notification Model
data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionUrl: String? = null
)

enum class NotificationType {
    INFO, SUCCESS, WARNING, ERROR, PROMO
}

// Settings Model
data class AppSettings(
    val darkModeEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val syncInterval: Int = 15, // minutes
    val biometricEnabled: Boolean = false,
    val language: String = "en",
    val accentColor: AccentColor = AccentColor.CYAN_PURPLE
)

enum class AccentColor {
    CYAN_PURPLE, BLUE_GREEN, PINK_ORANGE
}

// Storage Model
data class StorageInfo(
    val totalSpace: Long = 15L * 1024 * 1024 * 1024, // 15 GB
    val usedSpace: Long = 2500L * 1024 * 1024 // 2.5 GB
) {
    val freeSpace: Long get() = totalSpace - usedSpace
    val percentage: Float get() = usedSpace.toFloat() / totalSpace.toFloat()
}

// Dashboard Model
data class Dashboard(
    val user: UserProfile = UserProfile(),
    val projectsCount: Int = 0,
    val tasksCount: Int = 0,
    val completionPercentage: Int = 0,
    val recentProjects: List<Project> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val storageInfo: StorageInfo = StorageInfo()
)

// Network Response Wrapper
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
    class Loading<T> : Result<T>()
}
