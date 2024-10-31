package com.risyan.quickshutdownphone.feature.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.base.ui.theme.Body2Style
import com.risyan.quickshutdownphone.base.ui.theme.Header2Style
import com.risyan.quickshutdownphone.feature.navigator.EDU_SCREEN
import com.risyan.quickshutdownphone.feature.navigator.OnboardNavigator

fun NavGraphBuilder.EduScreen() {
    composable(route = EDU_SCREEN) {
        Content()
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun Content(
    modifier: Modifier = Modifier,
){
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ){
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ){

            ContentBlock(
                headerText = "Instant Lock Keeps You From Provoking Content",
                bodyText = "This app is designed to prevent prolonged exposure to arousing or NSFW content. If you find yourself in such situation and need to fight back this app will help you help escape without the need to move your finger.",
                imageResId = R.drawable.neuron_buster
            )

            ContentBlock(
                headerText = "Instant Lock Will Lock Your Phone Instantly",
                bodyText = "You wont be able to use your phone for several minutes if you happen to encounter NSFW or arousing content.",
                imageResId = R.drawable.lockdown_purpose_illustration
            )
        }
        Button(
            onClick = {
                OnboardNavigator.Current?.pop()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(text = "Back")
        }
    }

}

@Composable
fun ContentBlock(
    headerText: String,
    bodyText: String,
    imageResId: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = headerText,
            style = Header2Style
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = bodyText,
            style = Body2Style
        )
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            painter = painterResource(id = imageResId),
            contentDescription = "purpose"
        )
    }
}