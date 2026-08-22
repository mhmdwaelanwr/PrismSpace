@file:OptIn(ExperimentalFoundationApi::class)

package com.prismspace.container.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prismspace.container.ui.components.AddAppBottomSheet
import com.prismspace.container.ui.model.AppItem
import com.prismspace.container.ui.theme.CyanAccent
import com.prismspace.container.ui.theme.CyanToPurple
import com.prismspace.container.ui.theme.DarkBackground
import com.prismspace.container.ui.theme.DarkSurface
import com.prismspace.container.ui.theme.PrismSpaceTheme
import com.prismspace.container.ui.theme.TextPrimary
import com.prismspace.container.ui.theme.TextSecondary
import com.prismspace.container.ui.viewmodel.WorkspaceViewModel
import com.prismspace.container.ui.viewmodel.WorkspaceUiState

@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel = viewModel(),
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    var showAddAppSheet by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        val message = toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearToast()
    }
    
    WorkspaceContent(
        uiState = uiState,
        onSettingsClick = onSettingsClick,
        onAppClick = { packageName, _ -> viewModel.launchApp(packageName) },
        onLaunchFromMenuClick = { packageName -> viewModel.launchApp(packageName) },
        onStopRunningClick = { packageName -> viewModel.forceStopApp(packageName) },
        onClearDataClick = { packageName -> viewModel.clearClonedAppData(packageName) },
        onUninstallClick = { packageName -> viewModel.uninstallClonedApp(packageName) },
        onCreateShortcutClick = { packageName -> viewModel.createShortcut(packageName) },
        onAddAppClick = { showAddAppSheet = true }
    )
    
    // Bottom Sheet
    if (showAddAppSheet) {
        AddAppBottomSheet(
            viewModel = viewModel,
            onDismiss = { showAddAppSheet = false }
        )
    }
}

@Composable
fun WorkspaceContent(
    uiState: WorkspaceUiState,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onAppClick: (String, String) -> Unit = { _, _ -> },
    onLaunchFromMenuClick: (String) -> Unit = {},
    onStopRunningClick: (String) -> Unit = {},
    onClearDataClick: (String) -> Unit = {},
    onUninstallClick: (String) -> Unit = {},
    onCreateShortcutClick: (String) -> Unit = {},
    onAddAppClick: () -> Unit = {}
) {
    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            PrismSpaceTopAppBar(onSettingsClick = onSettingsClick)
        },
        floatingActionButton = {
            GradientFAB(
                onClick = onAddAppClick
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkBackground)
            ) {
                // AdMob Banner Placeholder
                AdBannerPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                
                // Render exactly one UI state at a time
                when (uiState) {
                    is WorkspaceUiState.Loading -> {
                        // Full-screen loading overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyanAccent)
                        }
                    }
                    
                    is WorkspaceUiState.Empty -> {
                        // Empty state - "No Apps Yet" centered
                        EmptyStateView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                    
                    is WorkspaceUiState.Success -> {
                        // Apps grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.apps) { app ->
                                AppGridItem(
                                    app = app,
                                    onAppClick = { onAppClick(app.packageName, app.appName) },
                                    menuExpanded = selectedAppForMenu?.packageName == app.packageName,
                                    onLongPress = {
                                        selectedAppForMenu = app
                                    },
                                    onDismissMenu = { selectedAppForMenu = null },
                                    onLaunch = {
                                        onLaunchFromMenuClick(app.packageName)
                                        selectedAppForMenu = null
                                    },
                                    onStopRunning = {
                                        onStopRunningClick(app.packageName)
                                        selectedAppForMenu = null
                                    },
                                    onClearData = {
                                        onClearDataClick(app.packageName)
                                        selectedAppForMenu = null
                                    },
                                    onUninstall = {
                                        onUninstallClick(app.packageName)
                                        selectedAppForMenu = null
                                    },
                                    onCreateShortcut = {
                                        onCreateShortcutClick(app.packageName)
                                        selectedAppForMenu = null
                                    }
                                )
                            }
                        }
                    }
                    
                    is WorkspaceUiState.Error -> {
                        // Error state
                        ErrorStateView(
                            message = uiState.message,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrismSpaceTopAppBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(brush = CyanToPurple, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Prism Space",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            actionIconContentColor = CyanAccent
        )
    )
}

@Composable
private fun AdBannerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = DarkSurface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = CyanAccent.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AdMob Banner",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: AppItem,
    onAppClick: () -> Unit,
    menuExpanded: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onLaunch: () -> Unit,
    onStopRunning: () -> Unit,
    onClearData: () -> Unit,
    onUninstall: () -> Unit,
    onCreateShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onLongPress
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // App Icon Card with Real Icon - now with ripple feedback
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconDrawable != null) {
                // Display real icon
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(app.iconDrawable)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = DarkSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            } else {
                // Fallback: use gradient background with first letter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = CyanToPurple,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Launch App", color = TextPrimary) },
                    onClick = onLaunch
                )
                DropdownMenuItem(
                    text = { Text(text = "Stop Running", color = TextPrimary) },
                    onClick = onStopRunning
                )
                DropdownMenuItem(
                    text = { Text(text = "Clear Data", color = TextPrimary) },
                    onClick = onClearData
                )
                DropdownMenuItem(
                    text = { Text(text = "Uninstall App", color = TextPrimary) },
                    onClick = onUninstall
                )
                DropdownMenuItem(
                    text = { Text(text = "Create Shortcut", color = TextPrimary) },
                    onClick = onCreateShortcut
                )
            }
        }
        
        // App Name
        Text(
            text = app.appName,
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = CyanToPurple,
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = Color.White,
                style = MaterialTheme.typography.displayLarge
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Apps Yet",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap the floating button to clone your apps",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorStateView(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                color = Color.Red,
                style = MaterialTheme.typography.displayLarge
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Unable to Load Apps",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GradientFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 8.dp, shape = CircleShape),
        containerColor = Color.Transparent,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = CyanToPurple,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add App",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun WorkspaceScreenPreview() {
    val sampleApps = listOf(
        AppItem("com.whatsapp", "WhatsApp", null, null, false),
        AppItem("com.facebook.katana", "Facebook", null, null, false),
        AppItem("com.instagram.android", "Instagram", null, null, false),
        AppItem("com.google.android.youtube", "YouTube", null, null, false),
        AppItem("com.android.chrome", "Chrome", null, null, false)
    )
    PrismSpaceTheme {
        WorkspaceContent(
            uiState = WorkspaceUiState.Success(sampleApps),
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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun WorkspaceScreenEmptyPreview() {
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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun WorkspaceScreenLoadingPreview() {
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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun WorkspaceScreenErrorPreview() {
    PrismSpaceTheme {
        WorkspaceContent(
            uiState = WorkspaceUiState.Error("Failed to load apps. Please try again later."),
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
