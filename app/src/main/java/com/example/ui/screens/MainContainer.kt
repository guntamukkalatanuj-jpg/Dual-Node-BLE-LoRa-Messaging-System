package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BluLinqViewModel
import com.example.ui.theme.BluBackgroundGradient
import com.example.ui.theme.BluBlack
import com.example.ui.theme.BluBorderSubtle
import com.example.ui.theme.BluCyan
import com.example.ui.theme.BluCyanNeon
import com.example.ui.theme.BluDarkNavy
import com.example.ui.theme.BluTextSecondary
import com.example.ui.theme.BluTextTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily

sealed class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    object Transmitters : NavTab("Transmitters", Icons.Default.Sensors, "tab_transmitters")
    object CentralUnit : NavTab("Central Unit", Icons.Default.Forum, "tab_central_unit")
    object Settings : NavTab("Settings", Icons.Default.Settings, "tab_settings")
}

@Composable
fun MainContainer(
    viewModel: BluLinqViewModel
) {
    var selectedTab by remember { mutableStateOf<NavTab>(NavTab.CentralUnit) }
    var hasRequestedPermissions by remember { mutableStateOf(false) }

    // Runtime Permission Launcher for Bluetooth & GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshLocation()
        viewModel.scanDevices()
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermissions) {
            hasRequestedPermissions = true
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BluBlack),
        bottomBar = {
            NavigationBar(
                containerColor = BluDarkNavy,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(BluCyan.copy(alpha = 0.35f), BluBorderSubtle)
                        ),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                val tabs = listOf(
                    NavTab.Transmitters,
                    NavTab.CentralUnit,
                    NavTab.Settings
                )

                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluBlack,
                            selectedTextColor = BluCyanNeon,
                            indicatorColor = BluCyanNeon,
                            unselectedIconColor = BluTextSecondary,
                            unselectedTextColor = BluTextTertiary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BluBackgroundGradient)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                is NavTab.Transmitters -> TransmitterScreen(viewModel = viewModel)
                is NavTab.CentralUnit -> ReceiverScreen(viewModel = viewModel)
                is NavTab.Settings -> DevicesScreen(viewModel = viewModel)
            }
        }
    }
}
