package com.storyspots.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun DisplayProgressBar() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/29ddadc3-e912-4dc9-a459-cf01e3127175/vHlPS49rI7.lottie")
    )

    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.size(100.dp)
    )
}