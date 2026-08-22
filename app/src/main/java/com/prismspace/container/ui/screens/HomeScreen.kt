package com.prismspace.container.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismspace.container.ui.components.*
import com.prismspace.container.ui.theme.*

/**
 * Premium Dark-Mode Home Screen
 * Features Material Design 3 with Cyan-to-Purple gradient accent
 */
@Composable
fun HomeScreen(
    onNavigateToDetails: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
    ) {
        // Gradient Header
        GradientHeader(
            title = "Prism Space",
            subtitle = "Premium Dark Mode Interface"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Featured Section
        FeaturedSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Navigation
        var tabState by remember { mutableIntStateOf(0) }
        GradientTabRow(
            tabs = listOf("Overview", "Activities", "Settings"),
            selectedTabIndex = tabState,
            onTabSelected = { tabState = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on selected tab
        when (tabState) {
            0 -> OverviewTabContent(onNavigateToDetails)
            1 -> ActivitiesTabContent()
            2 -> SettingsTabContent()
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeaturedSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(title = "Featured")

        Spacer(modifier = Modifier.height(12.dp))

        // Featured Card with gradient
        AnimatedGradientBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Experience Premium",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enjoy the ultimate dark-mode experience",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .widthIn(min = 100.dp)
                    ) {
                        Text(
                            "Learn More",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun OverviewTabContent(onNavigateToDetails: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Statistics Grid
        SectionHeader(title = "Statistics")
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticCard(
                value = "12",
                label = "Projects",
                modifier = Modifier.weight(1f),
                iconColor = PrimaryLight
            )
            StatisticCard(
                value = "48",
                label = "Tasks",
                modifier = Modifier.weight(1f),
                iconColor = SecondaryLight
            )
            StatisticCard(
                value = "89%",
                label = "Complete",
                modifier = Modifier.weight(1f),
                iconColor = TertiaryLight
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Features Grid
        SectionHeader(title = "Features")
        Spacer(modifier = Modifier.height(12.dp))

        val features = listOf(
            Pair("Settings", "Customize") to Icons.Default.Settings,
            Pair("Notifications", "Manage") to Icons.Default.Notifications,
            Pair("Storage", "Manage") to Icons.Default.Storage,
            Pair("Security", "Configure") to Icons.Default.Lock
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (feature, icon) ->
                        FeatureItem(
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            title = feature.first,
                            description = feature.second,
                            onClick = { onNavigateToDetails(feature.first) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Items
        SectionHeader(title = "Recent Items")
        Spacer(modifier = Modifier.height(12.dp))

        val recentItems = listOf(
            Triple("Premium Dashboard", "Updated 2 hours ago", "123 MB"),
            Triple("Cloud Sync Active", "Running in background", "45 MB"),
            Triple("Latest Backup", "Completed yesterday", "890 MB")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentItems.forEach { (title, subtitle, _) ->
                SwipeActionItem(
                    title = title,
                    subtitle = subtitle,
                    actionText = "Remove",
                    onAction = { /* Handle removal */ }
                )
            }
        }
    }
}

@Composable
private fun ActivitiesTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(title = "Recent Activity")
        Spacer(modifier = Modifier.height(12.dp))

        val activities = listOf(
            "Workspace updated" to "15 minutes ago",
            "New task added" to "1 hour ago",
            "File synchronized" to "3 hours ago",
            "Settings changed" to "Yesterday"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activities.forEach { (activity, time) ->
                ExpandableListItem(
                    title = activity,
                    description = time,
                    content = {
                        Text(
                            text = "This activity shows detailed information about the event that occurred.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Cards
        InfoCard(
            title = "Sync Status",
            message = "Your data is being synchronized",
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            },
            actionText = "Details",
            backgroundColor = Info
        )
    }
}

@Composable
private fun SettingsTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(title = "App Settings")
        Spacer(modifier = Modifier.height(12.dp))

        val settings = listOf(
            "Theme" to "Dark Mode",
            "Notifications" to "Enabled",
            "Auto-sync" to "Every 15 minutes",
            "Storage" to "Optimized"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            settings.forEach { (setting, value) ->
                PrismCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = setting,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        PrismGradientButton(
            text = "Save Changes",
            onClick = { /* Handle save */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { /* Handle reset */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ErrorRed
            ),
            border = ButtonDefaults.outlinedButtonBorder,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Reset to Default",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PrismSpaceTheme {
        HomeScreen()
    }
}
