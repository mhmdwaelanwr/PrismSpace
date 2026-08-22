package com.prismspace.container.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.prismspace.container.ui.model.AppItem
import com.prismspace.container.ui.theme.CyanAccent
import com.prismspace.container.ui.theme.CyanToPurple
import com.prismspace.container.ui.theme.DarkBackground
import com.prismspace.container.ui.theme.DarkSurface
import com.prismspace.container.ui.theme.PurpleAccent
import com.prismspace.container.ui.theme.TextPrimary
import com.prismspace.container.ui.theme.TextSecondary
import com.prismspace.container.ui.theme.PrismSpaceTheme
import com.prismspace.container.ui.viewmodel.WorkspaceViewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch

@Composable
fun AddAppBottomSheet(
    viewModel: WorkspaceViewModel,
    onDismiss: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredInstalledApps.collectAsState()
    val sortedApps = remember(filteredApps) { filteredApps.sortedBy { it.appName.lowercase() } }
    val coroutineScope = rememberCoroutineScope()

    var pendingInstallPackage by remember { mutableStateOf<String?>(null) }
    var isInstalling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshInstalledApps()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isInstalling,
            dismissOnClickOutside = !isInstalling,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(
                    color = DarkBackground.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Apps to Prism Space",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isInstalling,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) }
                )
                
                // Loading State
                if (isInstalling) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CyanAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (pendingInstallPackage != null) "Installing..." else "Processing...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Apps List
                Text(
                    text = "Available Apps (${sortedApps.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedApps) { app ->
                        PremiumAppListItem(
                            app = app,
                            enabled = !isInstalling,
                            onAddClick = {
                                if (isInstalling) return@PremiumAppListItem
                                pendingInstallPackage = app.packageName
                                isInstalling = true
                                coroutineScope.launch {
                                    try {
                                        val installed = viewModel.installApp(app.packageName)
                                        if (installed) {
                                            onDismiss()
                                        }
                                    } finally {
                                        isInstalling = false
                                        pendingInstallPackage = null
                                    }
                                }
                            }
                        )
                    }
                }
                
                // Close Button
                Button(
                    onClick = onDismiss,
                    enabled = !isInstalling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Close",
                        color = CyanAccent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumAppListItem(
    app: AppItem,
    enabled: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = DarkSurface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconDrawable != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(app.iconDrawable)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = CyanAccent,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        // App Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = app.appName,
                color = TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            Text(
                text = app.packageName,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        
        // Add Button
        Button(
            onClick = onAddClick,
            enabled = enabled,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = DarkBackground,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = DarkBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = androidx.compose.material3.MaterialTheme.typography.bodyMedium.fontSize
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            "Search apps...",
                            color = TextSecondary.copy(alpha = 0.6f),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
            
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppItem,
    enabled: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = DarkBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Icon Placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = PurpleAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1),
                color = CyanAccent,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
        }
        
        // App Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = app.appName,
                color = TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = app.packageName,
                color = TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
        
        // Add Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    brush = CyanToPurple,
                    shape = CircleShape
                )
                .clickable(enabled = enabled) { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add App",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============== PREVIEW COMPOSABLES ==============

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 600
)
@Composable
fun AddAppBottomSheetPreview() {
    PrismSpaceTheme {
        val mockApps = listOf(
            AppItem(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                isInstalled = false
            ),
            AppItem(
                packageName = "com.instagram.android",
                appName = "Instagram",
                isInstalled = false
            ),
            AppItem(
                packageName = "com.telegram",
                appName = "Telegram",
                isInstalled = false
            ),
            AppItem(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                isInstalled = false
            ),
            AppItem(
                packageName = "com.facebook.katana",
                appName = "Facebook",
                isInstalled = false
            )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground.copy(alpha = 0.97f), shape = RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Apps to Prism Space",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Search Bar
                SearchBar(
                    query = "",
                    onQueryChange = {}
                )
                
                // Apps Label
                Text(
                    text = "Available Apps (5)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                
                // Mock app items
                mockApps.forEach { app ->
                    PremiumAppListItem(
                        app = app,
                        enabled = true,
                        onAddClick = {}
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 120
)
@Composable
fun PremiumAppListItemPreview() {
    PrismSpaceTheme {
        PremiumAppListItem(
            app = AppItem(
                packageName = "com.whatsapp",
                appName = "WhatsApp Messenger",
                isInstalled = false
            ),
            enabled = true,
            onAddClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 360,
    heightDp = 60
)
@Composable
fun SearchBarPreview() {
    PrismSpaceTheme {
        SearchBar(
            query = "whats",
            onQueryChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
