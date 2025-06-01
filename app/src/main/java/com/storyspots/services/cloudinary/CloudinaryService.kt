package com.storyspots.services.cloudinary

import android.content.Context
import android.net.Uri
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import io.github.cdimascio.dotenv.dotenv

class CloudinaryService()
{
    private lateinit var observer: CloudinaryLifecycleObserver
    private lateinit var urlText: TextView
    private lateinit var context: Context

    fun onCreate(context: Context)
    {
        this.context = context

        val config = HashMap<String, String>()
        val dotenv = dotenv {
            directory = "assets"
            filename = "env"
        }

        config["cloud_name"] = dotenv["CLOUD_NAME"]
        config["api-key"] = dotenv["API_KEY"]
        config["api-secret"] = dotenv["API_SECRET"]

        MediaManager.init(this.context, config)
//        progressBar = findViewById(R.id.progressBar)

        this.observer = CloudinaryLifecycleObserver(requireActivity().activityResultRegistry)
        lifecycle.addObserver(this.observer)
    }

    fun onViewCreated(selectButton: Button)
    {
        selectButton.setOnClickListener {
            this.observer.pickImage()
        }
    }

    fun uploadImageToCloudinary(uri: Uri?, context: Context = this.context)
    {
        MediaManager.get().upload(uri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
//                    progressBar.isVisible = true
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    //in progress
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    Toast.makeText(context, "Image Uploaded", Toast.LENGTH_SHORT).show()
//                    urlText.text = resultData!!["url"] as String?
//                    progressBar.isVisible = false
//                    urlText.isVisible = true
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
//                    progressBar.isVisible = false
                    Toast.makeText(context, "Upload Error: ${error?.description}", Toast.LENGTH_LONG).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    //reschedule
                }
            })
            .dispatch()
    }
}