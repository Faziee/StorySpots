package com.storyspots.services.cloudinary

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

class ImageLoaderView
{
    @Composable
    fun DisplayProgressBar()
    {
        DotLottieAnimation(
            source = DotLottieSource.Url("https://lottie.host/29ddadc3-e912-4dc9-a459-cf01e3127175/vHlPS49rI7.lottie"),
            autoplay = true,
            loop = true,
            speed = 3f,
            useFrameInterpolation = false,
            playMode = Mode.Forward,
            modifier = Modifier.background(Color.Transparent)
        )
    }
}