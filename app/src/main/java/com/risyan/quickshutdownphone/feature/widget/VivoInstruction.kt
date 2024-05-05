package com.risyan.quickshutdownphone.feature.widget

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.feature.autoLaunchHonorSetting
import com.risyan.quickshutdownphone.feature.autoLaunchLetvSetting
import com.risyan.quickshutdownphone.feature.autoLaunchOppoSetting
import com.risyan.quickshutdownphone.feature.autoLaunchVivoSetting
import com.risyan.quickshutdownphone.feature.autoLaunchXiaomiSetting
import com.risyan.quickshutdownphone.feature.isHonorDevice
import com.risyan.quickshutdownphone.feature.isHuaweiDevice
import com.risyan.quickshutdownphone.feature.isLetvDevice
import com.risyan.quickshutdownphone.feature.isOppoDevice
import com.risyan.quickshutdownphone.feature.isVivoDevice
import com.risyan.quickshutdownphone.feature.isXiaomiDevice
import com.risyan.quickshutdownphone.feature.openUrl

@Composable
fun AutoSpecificInstruction() {
    val context = LocalContext.current
    if(context.isVivoDevice()){
        ManufactureSpecificInstruction(
            title = stringResource(R.string.vivo_android_autostart),
            description = stringResource(R.string.vivo_enable_autostart_one) +
                    stringResource(R.string.vivo_enable_autostart_two) +
                    stringResource(R.string.vivo_enable_autostart_three),
            openSettingString = stringResource(R.string.open_vivo_auto_start_settings),
        ) {
            context.autoLaunchVivoSetting()
        }
    }
    // xiaomi
    if (context.isXiaomiDevice()){
        ManufactureSpecificInstruction(
            title = stringResource(R.string.xiaomi_android_autostart),
            description = stringResource(R.string.vivo_enable_autostart_one) +
                    stringResource(R.string.vivo_enable_autostart_two) +
                    stringResource(R.string.vivo_enable_autostart_three),
            openSettingString = stringResource(R.string.open_vivo_auto_start_settings),
        ) {
            context.autoLaunchXiaomiSetting()
        }
    }

    // oppo
    if(context.isOppoDevice())
    {
        ManufactureSpecificInstruction(
            title = stringResource(R.string.oppo_android_autostart),
            description = stringResource(R.string.vivo_enable_autostart_one) +
                    stringResource(R.string.vivo_enable_autostart_two) +
                    stringResource(R.string.vivo_enable_autostart_three),
            openSettingString = stringResource(R.string.open_oppo_auto_start_settings),
        ) {
            context.autoLaunchOppoSetting()
        }
    }

    // letv
    if(context.isLetvDevice())
    {
        ManufactureSpecificInstruction(
            title = stringResource(R.string.letv_android_autostart),
            description = stringResource(R.string.vivo_enable_autostart_one) +
                    stringResource(R.string.vivo_enable_autostart_two) +
                    stringResource(R.string.vivo_enable_autostart_three),
            openSettingString = stringResource(R.string.open_let_auto_start_settings),
        ) {
            context.autoLaunchLetvSetting()
        }
    }

    // honor
    if(context.isHonorDevice())
    {
        ManufactureSpecificInstruction(
            title = stringResource(R.string.honor_android_autostart),
            description = stringResource(R.string.vivo_enable_autostart_one) +
                    stringResource(R.string.vivo_enable_autostart_two) +
                    stringResource(R.string.vivo_enable_autostart_three),
            openSettingString = stringResource(R.string.open_honor_auto_start_settings),
        ) {
            context.autoLaunchHonorSetting()
        }
    }

    // huawei
    if(context.isHuaweiDevice())
    {
        ManufactureSpecificInstruction(
            title = stringResource(R.string.huawei_android_autostart),
            description = stringResource(R.string.vivo_enable_autostart_one) +
                    stringResource(R.string.vivo_enable_autostart_two) +
                    stringResource(R.string.vivo_enable_autostart_three),
            openSettingString = stringResource(R.string.open_huawei_auto_start_settings),
        ) {
            context.isHuaweiDevice()
        }
    }

}



@Composable
@Preview
fun ManufactureSpecificInstruction(
    title: String = "",
    description: String = "",
    imageUrl: String = "",
    detailWebUrl: String = "",
    openSettingString: String = "",
    openDetailPageString: String = "",
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    Column {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            lineHeight = 28.sp,
            text = description,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (imageUrl.isNotBlank()) {
            val imageLoader = if (SDK_INT >= 28) {
                ImageDecoderDecoder.Factory()
            } else {
                GifDecoder.Factory()
            }
            val request = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .decoderFactory(imageLoader)
                .build()
            AsyncImage(
                model = request,
                contentDescription = "Vivo Autostart Illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (detailWebUrl.isNotBlank()) {
            Text(
                color = Color.Blue,
                style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                text = openDetailPageString,
                modifier = Modifier.clickable {
                    context.openUrl(detailWebUrl)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if(openSettingString.isNotBlank()) {
            Text(
                color = Color.Blue,
                style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                text = openSettingString,
                modifier = Modifier.clickable {
                    onOpenSettings()
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}