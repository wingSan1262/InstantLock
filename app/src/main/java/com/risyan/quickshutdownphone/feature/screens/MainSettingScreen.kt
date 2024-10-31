package com.risyan.quickshutdownphone.feature.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.risyan.quickshutdownphone.MyApp
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.base.ui.theme.DateStyle
import com.risyan.quickshutdownphone.base.ui.theme.Header3Style
import com.risyan.quickshutdownphone.base.ui.theme.HeaderStyle
import com.risyan.quickshutdownphone.feature.currentDatetoFormattedString
import com.risyan.quickshutdownphone.feature.navigator.MAIN_SETTING_SCREEN


fun NavGraphBuilder.MainSettingScreen(){
    composable(route = MAIN_SETTING_SCREEN) {
        val rememberUserSetting = remember {
            MyApp.getInstance().userSetting
        }
        MainScreenContent(
            rememberUserSetting.lockByNsfw,
            rememberUserSetting.lockBySexy,
            rememberUserSetting.nightTime
        ){ nsfw, sexy, night ->
            val new = rememberUserSetting.copy(
                lockByNsfw = nsfw,
                lockBySexy = sexy,
                nightTime = night
            )
            MyApp.getInstance().userLockSetting.saveUserSetting(
                new
            )
            MyApp.getInstance().userSetting.apply {
                lockByNsfw = nsfw
                lockBySexy = sexy
                nightTime = night
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MainScreenContent(
    nsfwMode: Boolean = true,
    sexyMode: Boolean = true,
    nightTimeMode : Boolean = false,
    onUpdate: (nfsw: Boolean, sexy: Boolean, night: Boolean) -> Unit = { _, _, _ -> }
) {

    var nsfwLockMode by remember { mutableStateOf(nsfwMode) }
    var sexyLockMode by remember { mutableStateOf(sexyMode) }
    var nightTimeLockMode by remember { mutableStateOf(nightTimeMode) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_monochrome),
                contentDescription = "app_icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = Header3Style
            )
        }
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = currentDatetoFormattedString(),
            style = DateStyle
        )
        Spacer(modifier = Modifier.height(32.dp))

        SwitchSetting(
            label = "Sexy Lock Mode",
            isChecked = sexyLockMode,
            onCheckedChange = {
                sexyLockMode = it
                onUpdate(nsfwLockMode, it, nightTimeLockMode)
            }
        )

        SwitchSetting(
            label = "Night Time Lock Mode",
            isChecked = nightTimeLockMode,
            onCheckedChange = {
                onUpdate(nsfwLockMode, sexyLockMode, it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please choose menu above",
            style = DateStyle.copy(
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SwitchSetting(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = HeaderStyle
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}