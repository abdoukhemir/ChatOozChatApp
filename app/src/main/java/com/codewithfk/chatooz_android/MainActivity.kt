package com.codewithfk.chatooz_android

import android.content.Intent
import android.os.Bundle
import android.util.Log // Added Log import
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth // Added FirebaseAuth import
import com.google.firebase.auth.ktx.auth // Added Firebase Auth KTX import
import com.google.firebase.ktx.Firebase // Added Firebase KTX import
import com.zegocloud.zimkit.services.ZIMKit
import im.zego.zim.enums.ZIMErrorCode


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth // Firebase Auth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase Auth
        auth = Firebase.auth

        Log.d("MainActivity", "MainActivity created.")
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) on starting the main activity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "User is logged in: ${currentUser.uid}. Attempting ZIMKit connection.")
            // Connect ZIMKit using Firebase Auth user info
            // Use the user's UID and Display Name
            connectUser(currentUser.uid, currentUser.displayName ?: "User") // Fallback username if displayName is null
        } else {
            // This case should ideally not happen if LoginActivity redirects correctly,
            // but as a fallback, redirect back to Login if no user is found.
            Log.w("MainActivity", "No user found in onStart. Redirecting to LoginActivity.")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity
        }
    }


    // This function now receives userId and userName from Firebase Auth
    fun connectUser(userId: String, userName: String) {
        Log.d("MainActivity", "Connecting ZIMKit for user: $userId with name: $userName")
        ZIMKit.connectUser(userId, userName, "https://storage.zego.im/IMKit/avatar/avatar-0.png") {
            if (it.code == ZIMErrorCode.SUCCESS) {
                Log.d("MainActivity", "ZIMKit connection successful.")
                toConversationActivity()
                // finish() // Decide if you want to finish MainActivity here
            } else {
                val msg = it.message ?: "Unknown ZIMKit connection error"
                // Corrected Log line: Log the code and message from the error object
                Log.e("MainActivity", "ZIMKit connection failed: Code ${it.code}, Message: $msg")
                runOnUiThread {
                    Toast.makeText(this, "ZIMKit connection failed: $msg", Toast.LENGTH_LONG).show()
                }
                // Handle connection failure - maybe show a retry option or redirect to login
            }
        }
    }

    private fun toConversationActivity() {
        Log.d("MainActivity", "Navigating to ConversationActivity.")
        // Redirect to the conversation list (Activity) you created.
        val intent = Intent(
            this,
            ConversationActivity::class.java
        )
        startActivity(intent)
    }
}