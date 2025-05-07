package com.codewithfk.chatooz_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.util.Log
import android.view.View // Import View for showLoading
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.codewithfk.chatooz_android.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database // Import KTX for database
import com.google.firebase.ktx.Firebase
import com.zegocloud.zimkit.services.ZIMKit // Import ZIMKit
import im.zego.zim.enums.ZIMErrorCode // Import ZIMErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val database: FirebaseDatabase by lazy { Firebase.database } // Use KTX for easier access
    private lateinit var imagePickerHelper: ImagePickerHelper

    private var selectedImageUri: Uri? = null
    private val defaultAvatar = "https://storage.zego.im/IMKit/avatar/avatar-0.png"

    private val readImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("SignUp", "Permission granted, launching image picker.")
            launchImagePicker()
        } else {
            Log.d("SignUp", "Permission denied.")
            showToast("Permission required to select photos")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagePickerHelper = ImagePickerHelper(this)
        setupViews()
        setupClickListeners()

        Log.d("SignUp", "SignUpActivity created.")
    }

    private fun setupViews() {
        binding.usernameEditText.filters = arrayOf(
            InputFilter { source, _, _, _, _, _ ->
                if (!source.matches(Regex("[a-zA-Z0-9_]+"))) "" else null
            },
            InputFilter.LengthFilter(20)
        )
    }

    private fun setupClickListeners() {
        binding.selectImageButton.setOnClickListener {
            Log.d("SignUp", "Select Image button clicked.")
            checkPermissionsAndPickImage()
        }

        binding.signUpButton.setOnClickListener {
            Log.d("SignUp", "Sign Up button clicked.")
            val email = binding.emailEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInputs(email, username, password)) {
                Log.d("SignUp", "Inputs validated. Proceeding to sign up.")
                signUpUser(email, password, username)
            } else {
                Log.d("SignUp", "Input validation failed.")
            }
        }

        // The loginLink listener is optional if Login is always the launcher, but kept here for now
        // binding.loginLink.setOnClickListener { // assuming id loginLink in layout
        //     Log.d("SignUp", "Login link clicked. Navigating to LoginActivity.")
        //     val intent = Intent(this, LoginActivity::class.java)
        //     startActivity(intent)
        //     // Optional: finish() SignUpActivity if you don't want it on the back stack
        // }
    }

    private fun checkPermissionsAndPickImage() {
        when {
            ContextCompat.checkSelfPermission(this, readImagePermission) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("SignUp", "Read permission already granted, launching image picker.")
                launchImagePicker()
            }
            shouldShowRequestPermissionRationale(readImagePermission) -> {
                Log.d("SignUp", "Showing permission rationale dialog.")
                showPermissionRationaleDialog()
            }
            else -> {
                Log.d("SignUp", "Requesting read permission.")
                requestPermissionLauncher.launch(readImagePermission)
            }
        }
    }

    private fun launchImagePicker() {
        imagePickerHelper.pickImage(
            onSelected = { uri ->
                selectedImageUri = uri
                Log.d("SignUp", "Image selected, URI: $uri")
                binding.profileImage.setImageURI(uri)
            },
            onError = { e ->
                Log.e("ImagePicker", "Error selecting image", e)
                showToast("Failed to select image")
            }
        )
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("Allow access to select a profile picture")
            .setPositiveButton("Allow") { _, _ ->
                Log.d("SignUp", "Rationale dialog: Allow clicked, requesting permission.")
                requestPermissionLauncher.launch(readImagePermission)
            }
            .setNegativeButton("Deny", null)
            .show()
    }

    private fun validateInputs(email: String, username: String, password: String): Boolean {
        var isValid = true
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Enter a valid email"
            isValid = false
        } else {
            binding.emailEditText.error = null // Clear error if valid
        }


        if (username.isEmpty() || username.length < 3) {
            binding.usernameEditText.error = "Username must be at least 3 characters"
            isValid = false
        } else {
            binding.usernameEditText.error = null // Clear error if valid
        }

        if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.passwordEditText.error = null // Clear error if valid
        }

        return isValid
    }


    private fun signUpUser(email: String, password: String, username: String) {
        showLoading(true)
        Log.d("SignUp", "Starting Firebase Auth user creation for email: $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SignUp", "Firebase Auth user created successfully.")
                    task.result.user?.let { firebaseUser ->
                        updateAuthProfile(firebaseUser, username)
                        handleUserCreation(firebaseUser.uid, username, email)
                        // ZIMKit connection and navigation happen AFTER user data is saved in handleUserCreation
                    } ?: run {
                        showLoading(false) // Hide loading if Firebase Auth user is null
                        Log.e("SignUp", "Firebase Auth user was null after successful creation task.")
                        showToast("User creation failed")
                    }
                } else {
                    showLoading(false) // Hide loading if Firebase Auth creation fails
                    Log.e("SignUp", "Firebase Auth user creation failed.", task.exception)
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun updateAuthProfile(user: FirebaseUser, username: String) {
        Log.d("SignUp", "Updating Auth profile for user: ${user.uid} with username: $username")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SignUp", "Auth profile updated successfully.")
            } else {
                Log.w("SignUp", "Failed to update auth profile", task.exception)
            }
        }
    }

    private fun handleUserCreation(userId: String, username: String, email: String) {
        Log.d("SignUp", "handleUserCreation called for userId: $userId. Selected image URI: $selectedImageUri")

        val imageUriToUpload = selectedImageUri

        CoroutineScope(Dispatchers.IO).launch {
            var finalImageUrl = defaultAvatar

            if (imageUriToUpload != null) {
                Log.d("SignUp", "Selected image URI is not null. Attempting Cloudinary upload.")
                val uploadResult = try {
                    CloudinaryUploader.uploadImage(imageUriToUpload, userId)
                } catch (e: Exception) {
                    Log.e("SignUp", "Error during Cloudinary upload helper call", e)
                    Result.failure<String>(e)
                }

                if (uploadResult.isSuccess) {
                    finalImageUrl = uploadResult.getOrThrow()
                    Log.d("SignUp", "Cloudinary upload successful. URL: $finalImageUrl")
                } else {
                    Log.e("SignUp", "Cloudinary upload failed, using default avatar.", uploadResult.exceptionOrNull())
                }
            } else {
                Log.d("SignUp", "No image selected. Using default avatar.")
            }

            withContext(Dispatchers.Main) {
                // Save user data to database
                saveUserToDatabase(userId, username, email, finalImageUrl)
                // ZIMKit connection and navigation happen AFTER saving to database
            }
        }
    }

    private fun saveUserToDatabase(userId: String, username: String, email: String, imageUrl: String) {
        Log.d("Database", "Attempting to save user $userId with image URL: $imageUrl")
        val user = User(
            uid = userId,
            username = username,
            email = email,
            profileImageUrl = imageUrl
        )

        database.reference.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("Database", "User data saved successfully for userId: $userId")
                sendEmailVerification()
                // Note: showLoading(false) is called here, before ZIMKit connect

                // Connect ZIMKit and navigate to ConversationActivity after successful sign up and data save
                connectZimKitAndNavigate(userId, username)

                // finish() // Finish SignUpActivity after successful flow
            }
            .addOnFailureListener { e ->
                showLoading(false) // Hide loading if database save fails
                Log.e("Database", "Failed to save user data for userId: $userId", e)
                showToast("Failed to save user data")
            }
    }

    // Function to connect ZIMKit and then navigate to ConversationActivity (Copied from LoginActivity)
    private fun connectZimKitAndNavigate(userId: String, userName: String) {
        Log.d("SignUpConnect", "Attempting to connect ZIMKit for user: $userId") // Added log tag for clarity

        // 1. Fetch the user's profile data from Firebase Realtime Database
        Firebase.database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) // Assuming you have a User data class

                    val profileImageUrl = user?.profileImageUrl ?: "https://storage.zego.im/IMKit/avatar/avatar-0.png" // Get the image URL or use default as fallback
                    val finalUserName = user?.username ?: userName // Use the username from DB if available, fallback to Auth display name

                    Log.d("SignUpConnect", "Fetched user data. Image URL: $profileImageUrl, Username: $finalUserName")

                    // 2. Connect ZIMKit using the fetched profileImageUrl and username
                    ZIMKit.connectUser(userId, finalUserName, profileImageUrl) { error -> // Pass fetched URL and username
                        if (error.code == ZIMErrorCode.SUCCESS) {
                            Log.d("SignUpConnect", "ZIMKit connection successful. Navigating to ConversationActivity.")
                            // 3. Navigate to ConversationActivity after ZIMKit connects
                            navigateToConversationActivity() // Make sure this function exists in this activity
                        } else {
                            val msg = error.message ?: "Unknown ZIMKit connection error"
                            Log.e("SignUpConnect", "ZIMKit connection failed: Code ${error.code}, Message: $msg")
                            runOnUiThread {
                                Toast.makeText(this@SignUpActivity, "ZIMKit connection failed: $msg", Toast.LENGTH_LONG).show()
                            }
                            // Decide how to handle connection failure here
                            // showLoading(false) // Ensure loading is hidden if ZIMKit fails
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle potential errors during database fetch
                    Log.e("SignUpConnect", "Failed to fetch user data from database: ${error.message}", error.toException())
                    runOnUiThread {
                        Toast.makeText(this@SignUpActivity, "Failed to get user profile: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                    // Fallback to connecting with default avatar if database fetch fails
                    Log.d("SignUpConnect", "Database fetch failed, attempting ZIMKit connect with default avatar.")
                    ZIMKit.connectUser(userId, userName, "https://storage.zego.im/IMKit/avatar/avatar-0.png") { error ->
                        if (error.code == ZIMErrorCode.SUCCESS) {
                            Log.d("SignUpConnect", "ZIMKit connection successful after DB fetch failed. Navigating to ConversationActivity.")
                            navigateToConversationActivity()
                        } else {
                            val msg = error.message ?: "Unknown ZIMKit connection error (fallback)"
                            Log.e("SignUpConnect", "ZIMKit connection failed (fallback): Code ${error.code}, Message: $msg")
                            runOnUiThread {
                                Toast.makeText(this@SignUpActivity, "ZIMKit connection failed: $msg", Toast.LENGTH_LONG).show()
                            }
                            // showLoading(false) // Ensure loading is hidden if fallback ZIMKit fails
                        }
                    }
                }
            })
    }

    private fun navigateToConversationActivity() {
        Log.d("SignUp", "Navigating to ConversationActivity.")
        val intent = Intent(this, ConversationActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Close SignUpActivity after successful flow completes
    }

    private fun sendEmailVerification() {
        Log.d("SignUp", "Sending email verification.")
        auth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener {
                Log.d("SignUp", "Email verification sent successfully.")
            }
            ?.addOnFailureListener { e ->
                Log.w("Verification", "Email verification failed", e)
            }
    }

    private fun handleSignUpError(exception: Exception?) {
        val errorMessage = when {
            exception?.message?.contains("email address is already in use") == true -> {
                Log.w("SignUp", "Sign up failed: Email already in use.")
                "Email already registered"
            }
            exception?.message?.contains("invalid email") == true -> {
                Log.w("SignUp", "Sign up failed: Invalid email.")
                "Invalid email format"
            }
            exception?.message?.contains("password is invalid") == true -> {
                Log.w("SignUp", "Sign up failed: Password too short.")
                "Password must be at least 6 characters"
            }
            else -> {
                Log.e("SignUp", "Sign up failed: Unknown error.", exception)
                "Sign up failed: ${exception?.message ?: "Unknown error"}"
            }
        }
        showToast(errorMessage)
    }

    private fun showLoading(show: Boolean) {
        Log.d("SignUp", "showLoading: $show")
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.signUpButton.isEnabled = !show
        binding.selectImageButton.isEnabled = !show
        binding.emailEditText.isEnabled = !show
        binding.usernameEditText.isEnabled = !show
        binding.passwordEditText.isEnabled = !show
        // binding.loginLink.isEnabled = !show // If you kept the link, manage its enabled state
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d("SignUp", "Showing Toast: $message")
    }
}