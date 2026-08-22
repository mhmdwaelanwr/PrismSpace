package com.prismspace.container.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prismspace.container.ui.components.*
import com.prismspace.container.ui.model.AppItem
import com.prismspace.container.ui.screens.DetailScreen
import com.prismspace.container.ui.screens.HomeScreen
import com.prismspace.container.ui.screens.SettingsScreen
import com.prismspace.container.ui.screens.WorkspaceContent
import com.prismspace.container.ui.viewmodel.WorkspaceUiState
import com.prismspace.container.ui.theme.PrismSpaceTheme
import com.prismspace.container.ui.theme.TextPrimary
import com.prismspace.container.ui.theme.TextSecondary

/**
 * Preview Composables for Design Preview in Android Studio
 * PHASE K: UI Correction & Performance Optimization
 */

// ============== WORKSPACE SCREEN PREVIEWS ==============

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_6"
)
@Composable
fun WorkspaceScreenPreview_WithApps() {
    PrismSpaceTheme {
        val mockApps = listOf(
            AppItem(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                isInstalled = true
            ),
            AppItem(
                packageName = "com.instagram",
                appName = "Instagram",
                isInstalled = true
            ),
            AppItem(
                packageName = "com.telegram",
                appName = "Telegram",
                isInstalled = true
            ),
            AppItem(
                packageName = "com.facebook",
                appName = "Facebook",
                isInstalled = true
            ),
            AppItem(
                packageName = "com.twitter",
                appName = "Twitter/X",
                isInstalled = true
            ),
            AppItem(
                packageName = "com.tiktok",
                appName = "TikTok",
                isInstalled = true
            )
        )
        
        WorkspaceContent(
            uiState = WorkspaceUiState.Success(mockApps),
            onSettingsClick = {},
            onAppClick = { _, _ -> },
            onLaunchFromMenuClick = {},
            onStopRunningClick = {},
            onClearDataClick = {},
            onUninstallClick = {},
            onCreateShortcutClick = {},
            onAddAppClick = {}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_6"
)
@Composable
fun WorkspaceScreenPreview_Empty() {
    PrismSpaceTheme {
        WorkspaceContent(
            uiState = WorkspaceUiState.Empty,
            onSettingsClick = {},
            onAppClick = { _, _ -> },
            onLaunchFromMenuClick = {},
            onStopRunningClick = {},
            onClearDataClick = {},
            onUninstallClick = {},
            onCreateShortcutClick = {},
            onAddAppClick = {}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_6"
)
@Composable
fun WorkspaceScreenPreview_Loading() {
    PrismSpaceTheme {
        WorkspaceContent(
            uiState = WorkspaceUiState.Loading,
            onSettingsClick = {},
            onAppClick = { _, _ -> },
            onLaunchFromMenuClick = {},
            onStopRunningClick = {},
            onClearDataClick = {},
            onUninstallClick = {},
            onCreateShortcutClick = {},
            onAddAppClick = {}
        )
    }
}

// ============== HOME SCREEN PREVIEWS ==============

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_6"
)
@Composable
fun HomeScreenPreview() {
    PrismSpaceTheme {
        HomeScreen()
    }
}

// ============== DETAIL SCREEN PREVIEWS ==============

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_6"
)
@Composable
fun DetailScreenPreview() {
    PrismSpaceTheme {
        DetailScreen(itemTitle = "Premium Feature")
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_6"
)
@Composable
fun SettingsScreenPreview() {
    PrismSpaceTheme {
        SettingsScreen()
    }
}

// Component Previews

@Preview(showBackground = true)
@Composable
fun PrismGradientButtonPreview() {
    PrismSpaceTheme {
        PrismGradientButton(
            text = "Click Me",
            onClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GradientHeaderPreview() {
    PrismSpaceTheme {
        GradientHeader(
            title = "Welcome Back",
            subtitle = "Your dashboard is ready"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PrismCardPreview() {
    PrismSpaceTheme {
        PrismCard {
            Box(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "This is a card",
                    color = TextPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeatureItemPreview() {
    PrismSpaceTheme {
        FeatureItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            title = "Settings",
            description = "Configure app"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatusBadgePreview() {
    PrismSpaceTheme {
        StatusBadge(text = "Premium")
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticCardPreview() {
    PrismSpaceTheme {
        StatisticCard(
            value = "48",
            label = "Tasks"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    PrismSpaceTheme {
        EmptyState(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            title = "No Items",
            description = "Start by creating your first item"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AnimatedGradientBoxPreview() {
    PrismSpaceTheme {
        AnimatedGradientBox {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Animated Box",
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpandableListItemPreview() {
    PrismSpaceTheme {
        ExpandableListItem(
            title = "Expandable Item",
            description = "Click to expand",
            content = {
                Text(
                    text = "This is expanded content",
                    color = TextSecondary
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GradientTabRowPreview() {
    PrismSpaceTheme {
        GradientTabRow(
            tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
            selectedTabIndex = 0,
            onTabSelected = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoCardPreview() {
    PrismSpaceTheme {
        InfoCard(
            title = "Information",
            message = "This is an informational card",
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )
    }
}
