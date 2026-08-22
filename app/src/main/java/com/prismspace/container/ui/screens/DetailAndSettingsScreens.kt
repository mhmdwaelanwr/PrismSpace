package com.prismspace.container.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismspace.container.ui.components.*
import com.prismspace.container.ui.theme.*

/**
 * Detail Screen for individual items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemTitle: String = "Premium Feature",
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
    ) {
        // Top Bar with back button
        TopAppBar(
            title = {
                Text(
                    text = itemTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryLight
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                scrolledContainerColor = SurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Hero Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(brush = DiagonalGradient)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
        }

        // Content Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = itemTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle/Badge
            StatusBadge(
                text = "Premium Feature",
                backgroundColor = PrimaryLight,
                textColor = BackgroundDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description Section
            SectionHeader(title = "About")
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This is a premium feature that provides advanced functionality and enhanced user experience. Experience the power of dark mode with Material Design 3.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features List
            SectionHeader(title = "Features")
            Spacer(modifier = Modifier.height(12.dp))

            val features = listOf(
                "Advanced Dark Mode Optimization",
                "Seamless Animations & Transitions",
                "Material Design 3 Components",
                "Cyan-to-Purple Gradient Accent",
                "Responsive Layout System",
                "OLED Friendly Colors"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SurfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = feature,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Specifications
            SectionHeader(title = "Specifications")
            Spacer(modifier = Modifier.height(12.dp))

            SpecificationRow(label = "Version", value = "1.0.0")
            SpecificationRow(label = "Status", value = "Active", valueColor = SuccessGreen)
            SpecificationRow(label = "Updated", value = "Today")
            SpecificationRow(label = "Size", value = "2.5 MB")

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            PrismGradientButton(
                text = "Use This Feature",
                onClick = { /* Handle action */ },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* Handle secondary action */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Learn More",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Card
            InfoCard(
                title = "Premium Member",
                message = "You have access to all premium features",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                backgroundColor = SecondaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpecificationRow(
    label: String,
    value: String,
    valueColor: Color = TextSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/**
 * Settings Screen with comprehensive options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onOpenPrivacyShield: () -> Unit = {},
    viewModel: com.prismspace.container.ui.viewmodel.PrismSpaceViewModel? = null
) {
    val scrollState = rememberScrollState()
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val isLoading = viewModel?.isLoading?.collectAsState()?.value ?: false
    val actionMessage = viewModel?.settingsActionMessage?.collectAsState()?.value
    val errorMessage = viewModel?.error?.collectAsState()?.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryLight
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Appearance Section
            SettingsSectionHeader(
                title = "Appearance",
                isExpanded = expandedSection == "appearance",
                onToggle = { expandedSection = if (expandedSection == "appearance") null else "appearance" }
            )

            AnimatedVisibility(visible = expandedSection == "appearance") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsToggleItem(
                        icon = Icons.Default.Brightness4,
                        title = "Dark Mode",
                        description = "Use dark theme",
                        isEnabled = true,
                        onToggle = { /* Handle dark mode toggle */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.Palette,
                        title = "Accent Color",
                        description = "Cyan to Purple",
                        onClick = { /* Handle color selection */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.TextFields,
                        title = "Text Size",
                        description = "Standard",
                        onClick = { /* Handle text size */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notifications Section
            SettingsSectionHeader(
                title = "Notifications",
                isExpanded = expandedSection == "notifications",
                onToggle = { expandedSection = if (expandedSection == "notifications") null else "notifications" }
            )

            AnimatedVisibility(visible = expandedSection == "notifications") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        description = "Receive alerts",
                        isEnabled = true,
                        onToggle = { /* Handle toggle */ }
                    )

                    SettingsToggleItem(
                        icon = Icons.Default.Email,
                        title = "Email Notifications",
                        description = "Receive email updates",
                        isEnabled = false,
                        onToggle = { /* Handle toggle */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.Schedule,
                        title = "Notification Time",
                        description = "9:00 AM - 9:00 PM",
                        onClick = { /* Handle time selection */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sync Section
            SettingsSectionHeader(
                title = "Sync & Storage",
                isExpanded = expandedSection == "sync",
                onToggle = { expandedSection = if (expandedSection == "sync") null else "sync" }
            )

            AnimatedVisibility(visible = expandedSection == "sync") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsToggleItem(
                        icon = Icons.Default.CloudSync,
                        title = "Auto Sync",
                        description = "Sync automatically",
                        isEnabled = true,
                        onToggle = { /* Handle toggle */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.Storage,
                        title = "Storage Used",
                        description = "2.5 GB of 15 GB",
                        onClick = { /* Handle storage */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.DeleteOutline,
                        title = "Clear Cache",
                        description = "Free up space",
                        onClick = { /* Handle clear */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy & Security
            SettingsSectionHeader(
                title = "Privacy & Security",
                isExpanded = expandedSection == "privacy",
                onToggle = { expandedSection = if (expandedSection == "privacy") null else "privacy" }
            )

            AnimatedVisibility(visible = expandedSection == "privacy") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsOptionItem(
                        icon = Icons.Default.Shield,
                        title = "Privacy Shield",
                        description = "Virtual location + device profile",
                        onClick = onOpenPrivacyShield
                    )

                    SettingsToggleItem(
                        icon = Icons.Default.Lock,
                        title = "Biometric Lock",
                        description = "Fingerprint/Face ID",
                        isEnabled = false,
                        onToggle = { /* Handle toggle */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.Security,
                        title = "App Password",
                        description = "Set a password",
                        onClick = { /* Handle password */ }
                    )

                    SettingsOptionItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        description = "Read our policy",
                        onClick = { /* Handle privacy */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            Text(
                text = "Danger Zone",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ErrorRed,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!actionMessage.isNullOrBlank()) {
                InfoCard(
                    title = "Action Complete",
                    message = actionMessage,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    backgroundColor = SecondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!errorMessage.isNullOrBlank()) {
                InfoCard(
                    title = "Action Failed",
                    message = errorMessage,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    backgroundColor = ErrorRed
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { viewModel?.forceStopAllRunningApps() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ErrorRed
                ),
                border = ButtonDefaults.outlinedButtonBorder,
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && viewModel != null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (isLoading) "Stopping..." else "Force Stop All",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel?.clearAllAppData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ErrorRed
                ),
                border = ButtonDefaults.outlinedButtonBorder,
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && viewModel != null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (isLoading) "Clearing..." else "Clear All App Data",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Info
            PrismCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Prism Space",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SpecificationRow(label = "Version", value = "4.0.0")
                    SpecificationRow(label = "Build", value = "400")
                    SpecificationRow(label = "License", value = "Premium")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.graphicsLayer(
                rotationZ = if (isExpanded) 180f else 0f
            )
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var state by remember { mutableStateOf(isEnabled) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = state,
            onCheckedChange = {
                state = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryLight,
                checkedTrackColor = PrimaryLight.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============== PREVIEW COMPOSABLES ==============

@androidx.compose.ui.tooling.preview.Preview(
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

@androidx.compose.ui.tooling.preview.Preview(
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
