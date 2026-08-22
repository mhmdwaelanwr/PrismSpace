package com.prismspace.container.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prismspace.container.core.EngineCapabilities
import com.prismspace.container.core.PrismEngineFacade
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineDiagnosticsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshVersion by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    // Reading the registry is side-effect free. refreshVersion intentionally participates in
    // this read so a completed engine initialization produces a fresh snapshot.
    @Suppress("UNUSED_VARIABLE")
    val refreshKey = refreshVersion
    val registry = EngineCapabilities.get()
    val facts = registry.deviceFacts()
    val statuses = registry.snapshot().entries.toList()
    val diagnosticText = registry.toDiagnosticText()

    val activeCount = statuses.count { it.value.state == EngineCapabilities.State.ACTIVE }
    val degradedCount = statuses.count { it.value.state == EngineCapabilities.State.DEGRADED }
    val failedCount = statuses.count { it.value.state == EngineCapabilities.State.FAILED }
    val unknownCount = statuses.count { it.value.state == EngineCapabilities.State.UNKNOWN }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Engine Diagnostics", fontWeight = FontWeight.Bold)
                        Text(
                            "Runtime truth, not assumptions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HealthSummaryCard(
                active = activeCount,
                degraded = degradedCount,
                failed = failedCount,
                unknown = unknownCount
            )

            DiagnosticsSection(title = "Device facts") {
                facts.forEach { (key, value) ->
                    FactRow(
                        label = key.toDisplayName(),
                        value = if (key == "pageSize") value.toPageSizeLabel() else value
                    )
                }
            }

            DiagnosticsSection(title = "Engine health") {
                statuses.forEach { (component, status) ->
                    CapabilityRow(component = component, status = status)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (refreshing) return@Button
                        refreshing = true
                        scope.launch {
                            PrismEngineFacade.initEngine(context.applicationContext)
                            refreshVersion++
                            refreshing = false
                        }
                    },
                    enabled = !refreshing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(if (refreshing) "Refreshing" else "Refresh")
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("PrismSpace engine diagnostics", diagnosticText))
                        Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Copy report")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Interpretation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ACTIVE means runtime health was observed. DEGRADED means a known fallback or observe-only mode is active. UNKNOWN means Prism has no runtime evidence yet; it is not treated as success.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DiagnosticsSection(title = "Raw report") {
                Text(
                    text = diagnosticText.trim(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HealthSummaryCard(
    active: Int,
    degraded: Int,
    failed: Int,
    unknown: Int
) {
    val overallState = when {
        failed > 0 -> "Needs attention"
        degraded > 0 -> "Operational with fallbacks"
        unknown > 0 -> "Still probing"
        else -> "Healthy"
    }
    val overallColor = when {
        failed > 0 -> MaterialTheme.colorScheme.error
        degraded > 0 -> Color(0xFFFFB74D)
        unknown > 0 -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF5AD7A0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(overallColor, RoundedCornerShape(50))
                )
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("Prism Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(overallState, color = overallColor, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric("Active", active, Color(0xFF5AD7A0))
                SummaryMetric("Degraded", degraded, Color(0xFFFFB74D))
                SummaryMetric("Failed", failed, MaterialTheme.colorScheme.error)
                SummaryMetric("Unknown", unknown, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DiagnosticsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CapabilityRow(
    component: EngineCapabilities.Component,
    status: EngineCapabilities.Status
) {
    val color = when (status.state) {
        EngineCapabilities.State.ACTIVE -> Color(0xFF5AD7A0)
        EngineCapabilities.State.DEGRADED -> Color(0xFFFFB74D)
        EngineCapabilities.State.FAILED -> MaterialTheme.colorScheme.error
        EngineCapabilities.State.DISABLED -> MaterialTheme.colorScheme.outline
        EngineCapabilities.State.SUPPORTED -> MaterialTheme.colorScheme.primary
        EngineCapabilities.State.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status.state) {
        EngineCapabilities.State.ACTIVE -> Icons.Default.CheckCircle
        EngineCapabilities.State.FAILED -> Icons.Default.Error
        else -> Icons.Default.Warning
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    component.name.toDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    status.state.name,
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (status.detail.isNotBlank()) {
                Text(
                    status.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.toDisplayName(): String =
    lowercase()
        .split('_')
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

private fun String.toPageSizeLabel(): String {
    val bytes = toLongOrNull() ?: return this
    if (bytes <= 0) return "unknown"
    return if (bytes % 1024L == 0L) "${bytes / 1024L} KB ($bytes B)" else "$bytes B"
}
