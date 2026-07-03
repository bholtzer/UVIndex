package com.bihstudio.uvindex.presentation.screens.permission

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.analytics.AnalyticsManager
import com.bihstudio.uvindex.presentation.theme.*
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onPermissionsHandled: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val analytics = remember { AnalyticsManager() }
    LaunchedEffect(Unit) { analytics.logScreen(AnalyticsManager.Events.SCREEN_PERMISSION) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            analytics.logEvent(AnalyticsManager.Events.LOCATION_GRANTED)
            viewModel.onLocationGranted()
        } else {
            analytics.logEvent(AnalyticsManager.Events.LOCATION_DENIED)
        }
    }

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { granted ->
            if (granted) {
                analytics.logEvent(AnalyticsManager.Events.NOTIFICATION_OPT_IN)
                viewModel.onNotificationGranted()
            }
            viewModel.onPermissionsHandled()
            onPermissionsHandled()
        }
    } else null

    val locationGranted = locationPermissions.permissions.any { it.status.isGranted }
    val notificationGranted = notifPermission?.status?.isGranted ?: true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightBlue, NightMid))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔐", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.permissions_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.permissions_intro),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            PermissionCard(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.location_permission),
                description = stringResource(R.string.location_permission_desc),
                granted = locationGranted,
                onRequest = { locationPermissions.launchMultiplePermissionRequest() }
            )

            Spacer(Modifier.height(16.dp))

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notification_permission),
                description = stringResource(R.string.notification_permission_desc),
                granted = notificationGranted,
                onRequest = null   // asked after location
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    if (!locationGranted) {
                        locationPermissions.launchMultiplePermissionRequest()
                    } else {
                        if (!notificationGranted) {
                            notifPermission?.launchPermissionRequest()
                        } else {
                            viewModel.onNotificationGranted()
                            onPermissionsHandled()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when {
                        !locationGranted -> stringResource(R.string.allow_location)
                        !notificationGranted -> stringResource(R.string.allow_notifications)
                        else -> stringResource(R.string.continue_btn)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightBlue
                )
            }

            if (locationGranted) {
                TextButton(onClick = {
                    viewModel.onPermissionsHandled()
                    onPermissionsHandled()
                }) {
                    Text(stringResource(R.string.skip_notifications), color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onRequest: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightMid)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) UVLow else SunGold,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(description, fontSize = 12.sp, color = TextSecondary)
            }
            if (granted) {
                Text("✓", color = UVLow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
