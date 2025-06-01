package com.storyspots.services.cloudinary

import android.app.Fragment
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.storyspots.R
import io.github.cdimascio.dotenv.dotenv

class CloudinaryService() : Fragment()
{
    private lateinit var progressBar: ProgressBar
    private lateinit var observer: CloudinaryLifecycleObserver

    override fun onCreate(savedStateHandle: Bundle?) {
        super.onCreate()

        val config = HashMap<String, String>()
        val dotenv = dotenv {
            directory = "/assets"
            filename = "env"
        }

        config["cloud_name"] = dotenv["CLOUD_NAME"]
        config["api-key"] = dotenv["API_KEY"]
        config["api-secret"] = dotenv["API_SECRET"]

        MediaManager.init(this, config)
        progressBar = findViewById(R.id.progressBar)

        observer = CloudinaryLifecycleObserver(requireActivity().activityResultRegistry)
        lifecycle.addObserver(observer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selectButton = view.findViewById<Button>(R.id.select_button)

        selectButton.setOnClickListener {
            observer.pickImage()
        }
    }

    fun uploadImageToCloudinary(uri: Uri)
    {
        MediaManager.get().upload(uri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    progressBar.isVisible = true
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {

                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    TODO("track context")
                    Toast.makeText(this@MainActivity, "Image Uploaded", Toast.LENGTH_SHORT).show()
                    urlText.text = resultData!!["url"] as String?
                    progressBar.isVisible = false
                    urlText.isVisible = true
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    progressBar.isVisible = false
                    Toast.makeText(this@MainActivity, "Upload Error: ${error?.description}", Toast.LENGTH_LONG).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {

                }

            })
            .dispatch()
    }
}