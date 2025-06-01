package com.storyspots.services.cloudinary

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CloudinaryLifecycleObserver(private val registry: ActivityResultRegistry) : DefaultLifecycleObserver
{
    private lateinit var getContent: ActivityResultLauncher<String>

    override fun onCreate(owner: LifecycleOwner)
    {
        super.onCreate(owner)

        getContent = registry.register("key", owner, GetContent()) { uri ->
            CloudinaryService().uploadImageToCloudinary(uri)
        }
    }

    fun pickImage()
    {
        getContent.launch("image/*")
    }
}