package com.risyan.quickshutdownphone.feature.screens

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.base.ui.theme.Green_4CAF50
import com.risyan.quickshutdownphone.feature.OnLifecycleEvent
import com.risyan.quickshutdownphone.feature.hasAccessibilityService
import com.risyan.quickshutdownphone.feature.hasAdminPermission
import com.risyan.quickshutdownphone.feature.hasNotificationAccess
import com.risyan.quickshutdownphone.feature.hasOverlayPermission
import com.risyan.quickshutdownphone.feature.hasStorageAccessNeededInstantLock
import com.risyan.quickshutdownphone.feature.navigator.OnboardNavigator
import com.risyan.quickshutdownphone.feature.navigator.SETUP_PERMISSION
import com.risyan.quickshutdownphone.feature.openAdminPermissionSetting
import com.risyan.quickshutdownphone.feature.openOverlayPermissionSetting
import com.risyan.quickshutdownphone.feature.requestAccessibilityService
import com.risyan.quickshutdownphone.feature.requestNotificationAccess
import com.risyan.quickshutdownphone.feature.requestStorageAccess

fun NavGraphBuilder.SetupScreen() {
    composable(route = SETUP_PERMISSION) {
        val context = LocalContext.current
        SetupContent(
            isNeedNotificaiton = SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            isHaveNotificationAccess = context.hasNotificationAccess(),
            isHaveAdminAccess = context.hasAdminPermission(),
            isHaveAcessibiltyService = context.hasAccessibilityService(),
            isHaveStorageAccess = context.hasStorageAccessNeededInstantLock(),
            isHaveOverlayAccess = context.hasOverlayPermission()
        )
    }
}

@Composable
@Preview
fun SetupContent(
    context: Context = LocalContext.current,
    isNeedNotificaiton: Boolean = false,
    isHaveNotificationAccess: Boolean = false,
    isHaveAdminAccess: Boolean = false,
    isHaveAcessibiltyService: Boolean = false,
    isHaveStorageAccess: Boolean = false,
    isHaveOverlayAccess: Boolean = false,
) {

    var isHaveNotificationPermission by remember {
        mutableStateOf(isHaveNotificationAccess)
    }

    var isHaveStoragePermission by remember {
        mutableStateOf(isHaveStorageAccess)
    }

    var isHaveAccessibiltyServicePermission by remember {
        mutableStateOf(isHaveAcessibiltyService)
    }

    var isHaveAdminPermission by remember {
        mutableStateOf(isHaveAdminAccess)
    }

    var isHaveOverlayPermission by remember {
        mutableStateOf(isHaveOverlayAccess)
    }

    LaunchedEffect(
        key1 = isHaveNotificationPermission,
        key2 = isHaveAdminPermission,
        key3 = isHaveAccessibiltyServicePermission,
    ) {
        if (
            isHaveAdminPermission && isHaveNotificationPermission &&
            isHaveAccessibiltyServicePermission && isHaveStoragePermission &&
            isHaveOverlayPermission
        ) OnboardNavigator.Current?.navigateToMainSettingPage()
    }

    LaunchedEffect(
        key1 = isHaveStoragePermission,
        key2 = isHaveOverlayPermission,
    ) {
        if (
            isHaveAdminPermission && isHaveNotificationPermission &&
            isHaveAccessibiltyServicePermission && isHaveStoragePermission &&
            isHaveOverlayPermission
        ) OnboardNavigator.Current?.navigateToMainSettingPage()
    }


    OnLifecycleEvent { owner, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            context.hasNotificationAccess().also { isHaveNotificationPermission = it }
            context.hasAdminPermission().also { isHaveAdminPermission = it }
            context.hasAccessibilityService().also { isHaveAccessibiltyServicePermission = it }
            context.hasStorageAccessNeededInstantLock().also { isHaveStoragePermission = it }
            context.hasOverlayPermission().also { isHaveOverlayPermission = it }
        }
    }

    Column {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.setup_access_and_permission),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.lockdown_instruction_title),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (isNeedNotificaiton) ListItem(
            headlineContent = {
                Text(
                    if (isHaveNotificationPermission) stringResource(R.string.notification_permission_granted)
                    else stringResource(R.string.need_notification_permission)
                )
            },
            trailingContent = {
                if (isHaveNotificationPermission) Icon(
                    Icons.Default.Check,
                    tint = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Green_4CAF50),
                    contentDescription = null,
                )
                else Button(onClick = {
                    context.requestNotificationAccess()
                }) {
                    Text(text = stringResource(R.string.grant))
                }
            },
        )


        ListItem(
            headlineContent = {
                Text(
                    if (isHaveOverlayPermission) "Overlay Permission Granted"
                    else "Need Overlay Permission"
                )
            },
            trailingContent = {
                if (isHaveOverlayPermission) Icon(
                    Icons.Default.Check,
                    tint = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Green_4CAF50),
                    contentDescription = null,
                )
                else Button(onClick = {
                    context.openOverlayPermissionSetting()
                }) {
                    Text(text = stringResource(id = R.string.grant))
                }
            },
        )

        ListItem(
            headlineContent = {
                Text(
                    if (isHaveStoragePermission) "Storage Permission Granted"
                    else "Need Storage Permission"
                )
            },
            trailingContent = {
                if (isHaveStoragePermission) Icon(
                    Icons.Default.Check,
                    tint = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Green_4CAF50),
                    contentDescription = null,
                )
                else Button(onClick = {
                    context.requestStorageAccess()
                }) {
                    Text(text = stringResource(id = R.string.grant))
                }
            },
        )



        ListItem(
            headlineContent = {
                Text(
                    if (isHaveAdminPermission) stringResource(R.string.admin_permission_granted)
                    else stringResource(R.string.need_admin_permission)
                )
            },
            trailingContent = {
                if (isHaveAdminPermission) Icon(
                    Icons.Default.Check,
                    tint = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Green_4CAF50),
                    contentDescription = null,
                )
                else Button(onClick = {
                    context.openAdminPermissionSetting()
                }) {
                    Text(text = stringResource(id = R.string.grant))
                }
            },
        )

        ListItem(
            headlineContent = {
                Text(
                    if (isHaveAccessibiltyServicePermission) "Accessibility Service Permission Granted"
                    else "Need Accessibility Service Permission"
                )
            },
            trailingContent = {
                if (isHaveAccessibiltyServicePermission) Icon(
                    Icons.Default.Check,
                    tint = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Green_4CAF50),
                    contentDescription = null,
                )
                else Button(onClick = {
                    if(context.hasStorageAccessNeededInstantLock()){
                        context.requestAccessibilityService()
                    }else {
                        Toast.makeText(context, "Please grant storage permission first", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(text = stringResource(id = R.string.grant))
                }
            },
        )
    }
}