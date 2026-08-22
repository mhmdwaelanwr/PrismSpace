package com.prismspace.container.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismspace.container.core.PrismEngineFacade
import com.prismspace.container.core.privacy.DeviceProfileManager
import com.prismspace.container.ui.components.InfoCard
import com.prismspace.container.ui.theme.BackgroundDark
import com.prismspace.container.ui.theme.ErrorRed
import com.prismspace.container.ui.theme.PrimaryLight
import com.prismspace.container.ui.theme.PrismSpaceTheme
import com.prismspace.container.ui.theme.SecondaryContainer
import com.prismspace.container.ui.theme.SurfaceDark
import com.prismspace.container.ui.theme.SurfaceVariant
import com.prismspace.container.ui.theme.TextPrimary
import com.prismspace.container.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldScreen(
    onBackClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var packageName by remember { mutableStateOf("") }
    var userIdText by remember { mutableStateOf("0") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var imeiText by remember { mutableStateOf("") }
    var macText by remember { mutableStateOf("") }
    var androidIdText by remember { mutableStateOf("") }
    var modelText by remember { mutableStateOf("") }
    var manufacturerText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scroll)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Privacy Shield",
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Target App", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Package Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = userIdText,
                        onValueChange = { userIdText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("User ID") },
                        singleLine = true
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Location Shield", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Latitude") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Longitude") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                errorMessage = null
                                statusMessage = null
                                val userId = userIdText.toIntOrNull() ?: 0
                                val lat = latitudeText.toDoubleOrNull()
                                val lon = longitudeText.toDoubleOrNull()
                                if (packageName.isBlank() || lat == null || lon == null) {
                                    errorMessage = "Enter package, latitude, and longitude first"
                                    return@launch
                                }
                                val ok = PrismEngineFacade.setVirtualLocation(packageName, lat, lon, userId)
                                if (ok) {
                                    statusMessage = "Virtual location updated"
                                } else {
                                    errorMessage = "Failed to set virtual location"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Location", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Device Identity Shield", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = imeiText,
                        onValueChange = { imeiText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("IMEI") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = macText,
                        onValueChange = { macText = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("MAC Address") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = androidIdText,
                        onValueChange = { androidIdText = it.lowercase() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Android ID") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manufacturerText,
                        onValueChange = { manufacturerText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Manufacturer") },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val randomProfile = PrismEngineFacade.generateRandomDeviceProfile()
                                    imeiText = randomProfile.imei.orEmpty()
                                    macText = randomProfile.mac.orEmpty()
                                    androidIdText = randomProfile.androidId.orEmpty()
                                    modelText = randomProfile.model.orEmpty()
                                    manufacturerText = randomProfile.manufacturer.orEmpty()
                                    statusMessage = "Random premium profile generated"
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Text("Magic", modifier = Modifier.padding(start = 8.dp))
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    statusMessage = null
                                    errorMessage = null
                                    if (packageName.isBlank()) {
                                        errorMessage = "Package name is required"
                                        return@launch
                                    }
                                    val profile = DeviceProfileManager.DeviceProfile(
                                        imei = imeiText.ifBlank { null },
                                        mac = macText.ifBlank { null },
                                        androidId = androidIdText.ifBlank { null },
                                        model = modelText.ifBlank { null },
                                        manufacturer = manufacturerText.ifBlank { null }
                                    )
                                    val ok = PrismEngineFacade.saveDeviceProfile(
                                        packageName = packageName,
                                        profile = profile,
                                        userId = userIdText.toIntOrNull() ?: 0
                                    )
                                    if (ok) {
                                        statusMessage = "Device profile saved"
                                    } else {
                                        errorMessage = "Failed to save device profile"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Text("Save", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                InfoCard(
                    title = "Success",
                    message = statusMessage.orEmpty(),
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    backgroundColor = SecondaryContainer
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                InfoCard(
                    title = "Action Failed",
                    message = errorMessage.orEmpty(),
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    backgroundColor = ErrorRed
                )
            }

            Text(
                text = "Premium spoof values are scoped per package + user.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyShieldScreenPreview() {
    PrismSpaceTheme {
        PrivacyShieldScreen()
    }
}
