package com.storyspots.services.cloudinary

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class CloudinaryService(private val context: Context) {

    sealed class UploadState {
        object Idle : UploadState()
        object Loading : UploadState()
        data class Progress(val bytes: Long, val totalBytes: Long) : UploadState()
        data class Success(val url: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    private val _uploadState = Channel<UploadState>()
    val uploadState = _uploadState.receiveAsFlow()

    init {
        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        val config = HashMap<String, String>()

        config["cloud_name"] = "dviaaly3l"
        config["api_key"] = "478996327969175"
        config["api_secret"] = "3zb2ocMQWoRzLX2-QykpkZ-x06M"

        MediaManager.init(context, config)
    }

    fun uploadImageToCloudinary(uri: Uri?) {
        if (uri == null) {
            _uploadState.trySend(UploadState.Error("Invalid image URI"))
            return
        }

        MediaManager.get().upload(uri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    _uploadState.trySend(UploadState.Loading)
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    _uploadState.trySend(UploadState.Progress(bytes, totalBytes))
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("url") as? String
                    if (url != null) {
                        _uploadState.trySend(UploadState.Success(url))
                    } else {
                        _uploadState.trySend(UploadState.Error("Failed to get image URL"))
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    val errorMessage = error?.description ?: "Unknown upload error"
                    _uploadState.trySend(UploadState.Error(errorMessage))
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    // Handle reschedule if needed
                }
            })
            .dispatch()
    }
}