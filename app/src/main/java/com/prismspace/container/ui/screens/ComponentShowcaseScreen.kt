package com.prismspace.container.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Component Showcase Screen - Displays all available UI components
 * Perfect for design review and testing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcaseScreen(
    onBackClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        item {
            TopAppBar(
                title = {
                    Text(
                        text = "Component Showcase",
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
        }

        // Buttons Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Buttons")
                Spacer(modifier = Modifier.height(12.dp))

                PrismGradientButton(
                    text = "Primary Gradient Button",
                    onClick = { }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrismGradientButton(
                    text = "Secondary Gradient",
                    onClick = { },
                    gradient = PurpleToCyanGradient
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLight
                        )
                    ) {
                        Text("Filled", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SecondaryLight
                        ),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text("Outlined", fontSize = 12.sp)
                    }

                    TextButton(onClick = { }) {
                        Text("Text", fontSize = 12.sp, color = PrimaryLight)
                    }
                }
            }
        }

        // Cards Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Cards")
                Spacer(modifier = Modifier.height(12.dp))

                PrismCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Elevated Card",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedGradientBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Animated Gradient Card",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Text Styles Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Typography")
                Spacer(modifier = Modifier.height(12.dp))

                Text("Display Large", style = MaterialTheme.typography.displayLarge)
                Text("Display Medium", style = MaterialTheme.typography.displayMedium)
                Text("Headline Large", style = MaterialTheme.typography.headlineLarge)
                Text("Title Large", style = MaterialTheme.typography.titleLarge)
                Text("Body Large", style = MaterialTheme.typography.bodyLarge)
                Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
                Text("Label Small", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Badges Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Badges & Status")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(text = "Premium", backgroundColor = PrimaryLight)
                    StatusBadge(text = "Active", backgroundColor = SuccessGreen)
                    StatusBadge(text = "Pending", backgroundColor = WarningOrange)
                    StatusBadge(text = "Error", backgroundColor = ErrorRed)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { index ->
                        StatisticCard(
                            value = "${(index + 1) * 10}",
                            label = "Item ${index + 1}",
                            modifier = Modifier
                                .width(120.dp)
                                .height(100.dp),
                            iconColor = listOf(
                                PrimaryLight,
                                SecondaryLight,
                                TertiaryLight
                            )[index]
                        )
                    }
                }
            }
        }

        // Progress Indicators Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Progress Indicators")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientCircularProgress(
                        progress = 0.3f,
                        size = 80
                    )

                    GradientCircularProgress(
                        progress = 0.65f,
                        size = 80
                    )

                    GradientCircularProgress(
                        progress = 0.95f,
                        size = 80
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = 0.7f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = PrimaryLight,
                    trackColor = SurfaceVariant
                )
            }
        }

        // Features Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Feature Items")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        title = "Feature 1",
                        description = "Description",
                        modifier = Modifier.weight(1f)
                    )

                    FeatureItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        title = "Feature 2",
                        description = "Description",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Lists Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "List Items")
                Spacer(modifier = Modifier.height(12.dp))

                ExpandableListItem(
                    title = "Expandable Item",
                    description = "Click to expand",
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Line 1", color = TextSecondary, fontSize = 12.sp)
                            Text("Line 2", color = TextSecondary, fontSize = 12.sp)
                            Text("Line 3", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SwipeActionItem(
                    title = "Swipe Item",
                    subtitle = "Swipe to reveal action",
                    actionText = "Action"
                )
            }
        }

        // Tab Navigation Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Tabs")
                Spacer(modifier = Modifier.height(12.dp))

                var selectedTab by remember { mutableStateOf(0) }
                GradientTabRow(
                    tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Info Cards Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Info Cards")
                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(
                    title = "Information",
                    message = "This is informational",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    backgroundColor = Info
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(
                    title = "Success",
                    message = "Operation completed successfully",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    backgroundColor = SuccessGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(
                    title = "Warning",
                    message = "Please review this information",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    backgroundColor = WarningOrange
                )
            }
        }

        // Empty State Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Empty State")
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    EmptyState(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        title = "No Items",
                        description = "Start by creating your first item"
                    )
                }
            }
        }

        // Loading Skeleton Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Loading State")
                Spacer(modifier = Modifier.height(12.dp))

                ShimmerLoadingSkeleton(height = 60)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLoadingSkeleton(height = 80)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLoadingSkeleton(height = 60)
            }
        }

        // Color Palette Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SectionHeader(title = "Color Palette")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "Cyan" to PrimaryLight,
                        "Purple" to SecondaryLight,
                        "Teal" to TertiaryLight,
                        "Success" to SuccessGreen,
                        "Warning" to WarningOrange,
                        "Error" to ErrorRed
                    ).forEach { (name, color) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(color, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ComponentShowcaseScreenPreview() {
    PrismSpaceTheme {
        ComponentShowcaseScreen()
    }
}
