package com.codewithfk.chatooz_android

import android.app.Application
import com.codewithfk.chatooz_android.utils.Keys.APP_ID
import com.codewithfk.chatooz_android.utils.Keys.APP_SIGN
import com.zegocloud.zimkit.services.ZIMKit
import com.cloudinary.android.MediaManager // Import MediaManager
import java.util.HashMap // Import HashMap

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ZIMKit Initialization
        ZIMKit.initWith(this, APP_ID, APP_SIGN);
        ZIMKit.initNotifications();


        val config = HashMap<String, String>()

        config["cloud_name"] = "dvfjcypxq"

        config["api_key"] = "777871634495529"

        config["secure"] = "true"

        try {
            MediaManager.init(this, config)


            android.util.Log.d("CloudinaryConfig", "Cloudinary initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryConfig", "Failed to initialize Cloudinary", e)
        }

    }
}