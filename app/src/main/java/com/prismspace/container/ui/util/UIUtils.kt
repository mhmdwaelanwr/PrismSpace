package com.prismspace.container.ui.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prismspace.container.ui.theme.*
import com.prismspace.container.ui.model.Priority
import com.prismspace.container.ui.model.ProjectStatus
import com.prismspace.container.ui.model.TaskStatus
import com.prismspace.container.ui.model.NotificationType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for the UI layer
 */

// ===== Color Utilities =====

/**
 * Get color based on priority level
 */
fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.LOW -> SuccessGreen
        Priority.MEDIUM -> WarningOrange
        Priority.HIGH -> ErrorRed
        Priority.URGENT -> Color(0xFFFF1744)
    }
}

/**
 * Get gradient brush based on status
 */
fun getStatusGradient(status: ProjectStatus): Brush {
    return when (status) {
        ProjectStatus.ACTIVE -> CyanToPurpleGradient
        ProjectStatus.COMPLETED -> Brush.linearGradient(
            colors = listOf(
                SuccessGreen,
                TertiaryLight
            )
        )
        ProjectStatus.ARCHIVED -> Brush.linearGradient(
            colors = listOf(
                TextSecondary,
                TextTertiary
            )
        )
        ProjectStatus.ON_HOLD -> Brush.linearGradient(
            colors = listOf(
                WarningOrange,
                Color(0xFFFF9800).copy(alpha = 0.6f)
            )
        )
    }
}

/**
 * Get notification tint color
 */
fun getNotificationColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.INFO -> Info
        NotificationType.SUCCESS -> SuccessGreen
        NotificationType.WARNING -> WarningOrange
        NotificationType.ERROR -> ErrorRed
        NotificationType.PROMO -> SecondaryLight
    }
}

// ===== Shape Utilities =====

/**
 * Get consistent rounded corner shape
 */
fun getShapeBySize(size: Int): RoundedCornerShape {
    return when (size) {
        1 -> RoundedCornerShape(4.dp)
        2 -> RoundedCornerShape(8.dp)
        3 -> RoundedCornerShape(12.dp)
        4 -> RoundedCornerShape(16.dp)
        5 -> RoundedCornerShape(24.dp)
        else -> RoundedCornerShape(12.dp)
    }
}

// ===== Date Utilities =====

/**
 * Format timestamp to human-readable string
 */
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000} minutes ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Format timestamp to date string
 */
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Format timestamp to time string
 */
fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ===== Storage Utilities =====

/**
 * Format bytes to human-readable size
 */
fun formatStorageSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes.toFloat() / (1024 * 1024 * 1024))
    }
}

/**
 * Get storage status color
 */
fun getStorageStatusColor(percentage: Float): Color {
    return when {
        percentage < 0.5f -> SuccessGreen
        percentage < 0.75f -> WarningOrange
        percentage < 0.9f -> Color(0xFFFF6F00)
        else -> ErrorRed
    }
}

// ===== Text Utilities =====

/**
 * Truncate text to max length with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        this.substring(0, maxLength - 3) + "..."
    } else {
        this
    }
}

/**
 * Get initials from name
 */
fun String.getInitials(): String {
    return this.split(" ")
        .filter { it.isNotEmpty() }
        .map { it.first().uppercaseChar() }
        .take(2)
        .joinToString("")
}

// ===== Validation Utilities =====

/**
 * Validate email format
 */
fun isValidEmail(email: String): Boolean {
    return email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
}

/**
 * Validate password strength
 */
fun getPasswordStrength(password: String): PasswordStrength {
    var strength = 0
    
    if (password.length >= 8) strength++
    if (password.matches(Regex(".*[A-Z].*"))) strength++
    if (password.matches(Regex(".*[a-z].*"))) strength++
    if (password.matches(Regex(".*[0-9].*"))) strength++
    if (password.matches(Regex(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*"))) strength++

    return when (strength) {
        0, 1 -> PasswordStrength.WEAK
        2 -> PasswordStrength.FAIR
        3 -> PasswordStrength.GOOD
        4 -> PasswordStrength.STRONG
        5 -> PasswordStrength.VERY_STRONG
        else -> PasswordStrength.WEAK
    }
}

enum class PasswordStrength {
    WEAK, FAIR, GOOD, STRONG, VERY_STRONG
}

// ===== Progress Utilities =====

/**
 * Clamp progress value between 0 and 1
 */
fun Float.clampProgress(): Float {
    return this.coerceIn(0f, 1f)
}

/**
 * Convert progress to percentage
 */
fun Float.toPercentage(): Int {
    return (this * 100).toInt().coerceIn(0, 100)
}

// ===== Animation Utilities =====

/**
 * Calculate animation delay for staggered animations
 */
fun getStaggerDelay(index: Int, delayMs: Int = 50): Long {
    return (index * delayMs).toLong()
}

// ===== Debug Utilities =====

/**
 * Log UI events in debug mode
 */
fun logUIEvent(event: String, details: String = "") {
    android.util.Log.d("PrismSpace_UI", "$event: $details")
}
