package com.prismspace.container.ui.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.Painter

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Painter? = null,
    val iconDrawable: Drawable? = null,
    val isInstalled: Boolean = false,
    val isRunning: Boolean = false
)



