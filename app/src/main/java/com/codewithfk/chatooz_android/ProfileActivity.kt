package com.codewithfk.chatooz_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Import View for showLoading visibility
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.zegocloud.zimkit.services.ZIMKit

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile) // Replace with your layout name if different

        auth = Firebase.auth

        val profileImageView: ImageView = findViewById(R.id.profileImageView)
        val usernameTextView: TextView = findViewById(R.id.usernameTextView)
        val emailTextView: TextView = findViewById(R.id.emailTextView)
        val logoutButton: Button = findViewById(R.id.logoutButton)

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            Log.d("ProfileActivity", "Loading profile for user: ${user.uid}")
            // Get user information from Firebase Realtime Database
            Firebase.database.reference.child("users").child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Using your custom User data class here
                        val userDetails = snapshot.getValue(User::class.java)
                        userDetails?.let {
                            Log.d("ProfileActivity", "User data found in DB. Username: ${it.username}, Email: ${it.email}")
                            // Accessing properties from your custom User class
                            usernameTextView.text = it.username
                            emailTextView.text = it.email
                            Glide.with(this@ProfileActivity)
                                .load(it.profileImageUrl)
                                .placeholder(R.drawable.ic_default_avatar) // Ensure you have this drawable resource
                                .error(R.drawable.ic_default_avatar) // Optional: Fallback if loading fails
                                .into(profileImageView)
                        } ?: run {
                            Log.w("ProfileActivity", "User data is null in database snapshot for UID: ${user.uid}.")
                            Toast.makeText(this@ProfileActivity, "User data not found in database", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ProfileActivity", "Database error loading user profile: ${error.message}", error.toException())
                        Toast.makeText(this@ProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } ?: run {
            Log.w("ProfileActivity", "No current Firebase user found in ProfileActivity.")
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            // Optionally redirect to login if no user is found
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }


        logoutButton.setOnClickListener {
            Log.d("ProfileActivity", "Logout button clicked. Initiating logout.")
            auth.signOut() // Sign out from Firebase
            ZIMKit.disconnectUser() // Important : Also log out from ZIMKit (Correct method name)
            // Navigate back to LoginActivity and clear the back stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Close ProfileActivity
            Log.d("ProfileActivity", "User logged out and navigating to LoginActivity.")
        }
    }
}