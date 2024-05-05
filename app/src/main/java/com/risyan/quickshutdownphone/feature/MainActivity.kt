package com.risyan.quickshutdownphone.feature

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.feature.widget.AutoSpecificInstruction
import com.risyan.quickshutdownphone.ui.theme.Green_4CAF50
import com.risyan.quickshutdownphone.ui.theme.QuickShutdownPhoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickShutdownPhoneTheme {
                var isPermissionGrantedAll by remember {
                    mutableStateOf(hasAdminPermission() && hasNotificationAccess() && hasOverlayPermission())
                }
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (
                        !isPermissionGrantedAll
                    )
                        SettingsScreen(
                            isNeedNotificaiton = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                            isHaveNotificationAccess = hasNotificationAccess(),
                            isHaveAdminAccess = hasAdminPermission(),
                            isHaveOverlayPermission = hasOverlayPermission()
                        ) {
                            isPermissionGrantedAll = true
                        }
                    else{
                        InstructionComponent() {
                            this.finish()
                        }
                        startSingleAllBroadcastStarters()
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SettingsScreen(
    context: Context = LocalContext.current,
    isNeedNotificaiton: Boolean = false,
    isHaveNotificationAccess: Boolean = false,
    isHaveAdminAccess: Boolean = false,
    isHaveOverlayPermission: Boolean = false,
    onDone: () -> Unit = {}
) {


    var isHaveNotificationPermission by remember {
        mutableStateOf(isHaveNotificationAccess)
    }

    var isHaveAdminPermission by remember {
        mutableStateOf(isHaveAdminAccess)
    }

    var isHaveOverlayPerm by remember {
        mutableStateOf(isHaveOverlayPermission)
    }

    var isIntroductionDone by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        key1 = isHaveNotificationPermission,
        key2 = isHaveAdminPermission,
        key3 = isHaveOverlayPerm
    ) {
        if (isHaveAdminPermission && isHaveNotificationPermission && isHaveOverlayPerm)
            onDone()
    }


    OnLifecycleEvent { owner, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            context.hasNotificationAccess().also { isHaveNotificationPermission = it }
            context.hasAdminPermission().also { isHaveAdminPermission = it }
            context.hasOverlayPermission().also { isHaveOverlayPerm = it }
        }
    }

    Column {
        Spacer(modifier = Modifier.height(40.dp))
        if (!isIntroductionDone) {
            Text(
                text = stringResource(R.string.welcome_message),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                modifier = Modifier
                    .width(250.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.baseline_screen_lock_portrait_24),
                contentDescription = "illustration"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_description),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { isIntroductionDone = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(text = stringResource(R.string.continue_to_setup))
            }
        } else {
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
            if (isNeedNotificaiton)
                ListItem(
                    headlineContent = {
                        Text(
                            if (isHaveNotificationPermission) stringResource(R.string.notification_permission_granted)
                            else stringResource(R.string.need_notification_permission)
                        )
                    },
                    trailingContent = {
                        if (isHaveNotificationPermission)
                            Icon(
                                Icons.Default.Check,
                                tint = Color.White,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Green_4CAF50),
                                contentDescription = null,
                            )
                        else
                            Button(onClick = {
                                context.requestNotificationAccess()
                            }) {
                                Text(text = stringResource(R.string.grant))
                            }
                    },
                )

            ListItem(
                headlineContent = {
                    Text(
                        if (isHaveOverlayPerm) stringResource(R.string.overlay_permission_granted)
                        else stringResource(R.string.need_overlay_permission)
                    )
                },
                trailingContent = {
                    if (isHaveOverlayPerm)
                        Icon(
                            Icons.Default.Check,
                            tint = Color.White,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Green_4CAF50),
                            contentDescription = null,
                        )
                    else
                        Button(
                            onClick = {
                                context.openOverlayPermissionSetting()
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
                    if (isHaveAdminPermission)
                        Icon(
                            Icons.Default.Check,
                            tint = Color.White,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Green_4CAF50),
                            contentDescription = null,
                        )
                    else
                        Button(
                            onClick = {
                                context.openAdminPermissionSetting()
                            }) {
                            Text(text = stringResource(id = R.string.grant))
                        }
                },
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun InstructionComponent(
    context: Context = LocalContext.current,
    closeApp: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(
                rememberScrollState()
            )
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.your_device_is) +
                    (if(context.isManufactureAdditionalSetting())
                        stringResource(R.string.almost) else "") +
                    stringResource(R.string.ready_for_instant_lock),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(CircleShape),
            horizontalArrangement = Arrangement.Center
        ){
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ){
                Image(
                    painter = painterResource(id = R.drawable.baseline_lock_clock_24),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    alignment = Alignment.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            textAlign = TextAlign.Justify,
            text = stringResource(R.string.lockdown_remark),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = stringResource(R.string.operations), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.instruction_no_one), fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.instruction_dialog_shown), fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.minute_lock_instruction), fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.info_ic_24), contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.cannot_cancel_lock),
                fontSize = 14.sp
            )
        }
        if (context.isManufactureAdditionalSetting()) {
            Spacer(modifier = Modifier.height(32.dp))
            AutoSpecificInstruction()
        }

        Spacer(modifier = Modifier.height(52.dp))
        Text(
            modifier =  Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.widget_will_reappear),
            fontSize = 14.sp
        )
        Button(
            onClick = { closeApp() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.close_and_start)
            )
        }
    }
}