package com.example.quickshutdownphone.feature

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.example.quickshutdownphone.R
import com.example.quickshutdownphone.ui.theme.Green_4CAF50
import com.example.quickshutdownphone.ui.theme.QuickShutdownPhoneTheme

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
                    else
                        InstructionComponent() {
                            this.finish()
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
                text = "Welcome to Instant Lockdown",
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
                text = "Instant Lockdown is designed to help you focus and combat digital distractions. It allows you to instantly lock your phone for a set period of time.",
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
                Text(text = "Continue to Setup")
            }
        } else {
            Text(
                text = "Setup Access and permission",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Before you can lockdown your phone instantly. You need to setup few things.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isNeedNotificaiton)
                ListItem(
                    headlineContent = {
                        Text(
                            if (isHaveNotificationPermission) "Notification permission granted"
                            else "Need notification permission"
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
                                Text(text = "Grant")
                            }
                    },
                )

            ListItem(
                headlineContent = {
                    Text(
                        if (isHaveOverlayPerm) "Overlay permission granted"
                        else "Need overlay permission"
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
                            Text(text = "Grant")
                        }
                },
            )

            ListItem(
                headlineContent = {
                    Text(
                        if (isHaveAdminPermission) "Admin permission granted"
                        else "Need admin permission"
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
                            Text(text = "Grant")
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
            text = "Your Device is Ready for Instant Lock",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context).data(data = R.drawable.phone_lock).apply(block = {
                    size(260, 260)
                }).build(), imageLoader = imageLoader
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            alignment = Alignment.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            textAlign = TextAlign.Justify,
            text = "To do Instant Lockdown, you need to press the power button with a 0.2-second interval multiple times. You can do this on any state as long the phone is on.",
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Operations:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "1. Press 4 times for a 5-minute lock.", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "2. Press 6 times for a 10-minute lock.", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "3. Press > 8 times for a 30-minute lock.", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.info_ic_24), contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "You cannot cancel the lock once started.",
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(52.dp))
        Button(
            onClick = { closeApp() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(text = "Use Phone and Start Locking")
        }
    }
}