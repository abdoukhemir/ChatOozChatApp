package com.codewithfk.chatooz_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Import View for showLoading
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codewithfk.chatooz_android.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database // Import KTX for database
import com.google.firebase.ktx.Firebase
import com.zegocloud.zimkit.services.ZIMKit // Import ZIMKit
import im.zego.zim.enums.ZIMErrorCode // Import ZIMErrorCode


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        setupClickListeners()

        Log.d("Login", "LoginActivity created.")
    }

    // Check if user is already logged in and connect ZIMKit if so
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser // Check if a user is currently logged in
        if (currentUser != null) {
            // If a user is found, log them in via ZIMKit and navigate
            Log.d("Login", "User already logged in: ${currentUser.uid}.")
            connectZimKitAndNavigate(currentUser.uid, currentUser.displayName ?: "User")
        } else {
            // If no user is found, stay on the Login screen
            Log.d("Login", "No user currently logged in.")
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            Log.d("Login", "Login button clicked.")
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInputs(email, password)) {
                Log.d("Login", "Inputs validated. Attempting login.")
                signInUser(email, password)
            } else {
                Log.d("Login", "Input validation failed.")
            }
        }

        binding.signUpLink.setOnClickListener {
            Log.d("Login", "Sign Up link clicked. Navigating to SignUpActivity.")
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Enter a valid email"
            isValid = false
        } else {
            binding.emailEditText.error = null // Clear error if valid
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password cannot be empty"
            isValid = false
        } else {
            binding.passwordEditText.error = null // Clear error if valid
        }

        return isValid
    }

    private fun signInUser(email: String, password: String) {
        showLoading(true)
        Log.d("Login", "Attempting to sign in user with email: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false) // Hide loading once Firebase Auth task completes
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("Login", "signInWithEmailAndPassword successful")
                    val user = auth.currentUser
                    user?.let {
                        Log.d("Login", "User logged in: ${it.uid}. Attempting ZIMKit connection.")
                        // Connect ZIMKit after successful Firebase login
                        connectZimKitAndNavigate(it.uid, it.displayName ?: "User")
                    } ?: run {
                        Log.e("Login", "signInWithEmailAndPassword successful but user is null")
                        showToast("Login failed: User not found")
                    }
                } else {
                    // If sign in fails
                    Log.w("Login", "signInWithEmailAndPassword failed", task.exception)
                    handleLoginError(task.exception)
                }
            }
    }

    // Function to connect ZIMKit and then navigate to ConversationActivity
    private fun connectZimKitAndNavigate(userId: String, userName: String) {
        Log.d("LoginConnect", "Attempting to connect ZIMKit for user: $userId")

        // 1. Fetch the user's profile data from Firebase Realtime Database
        Firebase.database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) // Assuming you have a User data class User(val uid: String = "", val username: String = "", val email: String = "", val profileImageUrl: String = "")

                    val profileImageUrl = user?.profileImageUrl ?: "https://storage.zego.im/IMKit/avatar/avatar-0.png" // Get the image URL or use default as fallback
                    val finalUserName = user?.username ?: userName // Use the username from DB if available, fallback to Auth display name

                    Log.d("LoginConnect", "Fetched user data. Image URL: $profileImageUrl, Username: $finalUserName")

                    // 2. Connect ZIMKit using the fetched profileImageUrl and username
                    ZIMKit.connectUser(userId, finalUserName, profileImageUrl) { error -> // Pass fetched URL and username
                        if (error.code == ZIMErrorCode.SUCCESS) {
                            Log.d("LoginConnect", "ZIMKit connection successful. Navigating to ConversationActivity.")
                            // 3. Navigate to ConversationActivity after ZIMKit connects
                            navigateToConversationActivity() // Make sure this function exists in this activity
                        } else {
                            val msg = error.message ?: "Unknown ZIMKit connection error"
                            Log.e("LoginConnect", "ZIMKit connection failed: Code ${error.code}, Message: $msg")
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "ZIMKit connection failed: $msg", Toast.LENGTH_LONG).show()
                            }
                            // Decide how to handle connection failure here
                            // showLoading(false) // Ensure loading is hidden if ZIMKit fails
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle potential errors during database fetch
                    Log.e("LoginConnect", "Failed to fetch user data from database: ${error.message}", error.toException())
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Failed to get user profile: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                    // Fallback to connecting with default avatar if database fetch fails
                    Log.d("LoginConnect", "Database fetch failed, attempting ZIMKit connect with default avatar.")
                    ZIMKit.connectUser(userId, userName, "https://storage.zego.im/IMKit/avatar/avatar-0.png") { error ->
                        if (error.code == ZIMErrorCode.SUCCESS) {
                            Log.d("LoginConnect", "ZIMKit connection successful after DB fetch failed. Navigating to ConversationActivity.")
                            navigateToConversationActivity()
                        } else {
                            val msg = error.message ?: "Unknown ZIMKit connection error (fallback)"
                            Log.e("LoginConnect", "ZIMKit connection failed (fallback): Code ${error.code}, Message: $msg")
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "ZIMKit connection failed: $msg", Toast.LENGTH_LONG).show()
                            }
                            // showLoading(false) // Ensure loading is hidden if fallback ZIMKit fails
                        }
                    }
                }
            })
    }


    private fun navigateToConversationActivity() {
        // Navigate to the ConversationActivity
        val intent = Intent(this, ConversationActivity::class.java)
        // Add flags to clear the back stack so the user cannot go back to Login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Close LoginActivity
    }

    private fun handleLoginError(exception: Exception?) {
        val errorMessage = when {
            exception?.message?.contains("There is no user record corresponding to this identifier") == true ||
                    exception?.message?.contains("no user") == true -> { // Added check for "no user" which is common
                Log.w("Login", "Login failed: No user found.")
                "No account found with this email"
            }
            exception?.message?.contains("The email address is badly formatted") == true ||
                    exception?.message?.contains("invalid email") == true -> { // Added check for "invalid email"
                Log.w("Login", "Login failed: Badly formatted email.")
                "Invalid email format"
            }
            exception?.message?.contains("The password is not valid") == true ||
                    exception?.message?.contains("wrong password") == true -> { // Added check for "wrong password"
                Log.w("Login", "Login failed: Invalid password.")
                "Incorrect password"
            }
            else -> {
                Log.e("Login", "Login failed: Unknown error.", exception)
                "Login failed: ${exception?.message ?: "Unknown error"}"
            }
        }
        showToast(errorMessage)
    }

    private fun showLoading(show: Boolean) {
        Log.d("Login", "showLoading: $show")
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
        binding.emailEditText.isEnabled = !show
        binding.passwordEditText.isEnabled = !show
        binding.signUpLink.isEnabled = !show // Disable link while loading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d("Login", "Showing Toast: $message")
    }
}