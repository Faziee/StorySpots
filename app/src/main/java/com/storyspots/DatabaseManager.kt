package com.storyspots

import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class DatabaseManager {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO: Database
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
//
//        //TODO: Permissions Management
//        if (PermissionsManager.areLocationPermissionsGranted(this)) {
//            Log.d(TAG, "Location permission already granted")
//            locationPermissionGranted.value = true
//            initializeContent()
//        } else {
//            permissionsManager = PermissionsManager(this)
//            permissionsManager.requestLocationPermissions(this)
//        }
    }
}