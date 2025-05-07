package com.codewithfk.chatooz_android

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


// A simple wrapper for Cloudinary upload using coroutines
object CloudinaryUploader {

    private const val TAG = "CloudinaryUploader"
    // You must create an unsigned upload preset in your Cloudinary console
    // Replace "YOUR_UNSIGNED_UPLOAD_PRESET" with the name of your preset
    private const val UNSIGNED_UPLOAD_PRESET = "ml_default"

    suspend fun uploadImage(imageUri: Uri, userId: String): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get()
                .upload(imageUri)
                // Use an unsigned upload preset for client-side uploads
                .unsigned(UNSIGNED_UPLOAD_PRESET)
                // Set a unique public ID (optional but recommended)
                .option("public_id", "profile_images/${userId}") // Save under profile_images/{userId}
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started for request: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes) * 100
                        Log.d(TAG, "Upload progress for request $requestId: ${"%.2f".format(progress)}%")
                    }

                    override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                        if (url != null) {
                            Log.d(TAG, "Upload successful for request $requestId. URL: $url")
                            // Resume the coroutine with the success result (the URL)
                            continuation.resume(Result.success(url))
                        } else {
                            Log.e(TAG, "Upload successful but URL not found in response for request $requestId")
                            // Resume with a failure result if URL is missing
                            continuation.resume(Result.failure(IllegalStateException("Uploaded successfully but received no URL")))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo?) {
                        // Get the error description safely, provide a default message if error is null
                        val errorMessage = error?.getDescription() ?: "Cloudinary upload failed with no specific error description"
                        Log.e(TAG, "Upload failed for request $requestId: $errorMessage")

                        // Resume with a failure result, using the error description in the exception
                        // We can no longer include the specific underlying exception if getException() doesn't exist
                        continuation.resume(Result.failure(Exception("Cloudinary upload failed: $errorMessage")))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo?) {
                        // Get the error description safely, provide a default message if error is null
                        val rescheduleMessage = error?.getDescription() ?: "Cloudinary upload rescheduled with no specific reason description"
                        Log.w(TAG, "Upload rescheduled for request $requestId: $rescheduleMessage")

                        // Depending on requirements, you might resume with failure or let it retry.
                        // For now, we'll treat a reschedule as a failure that needs attention, using the description.
                        // We can no longer include the specific underlying exception if exception property doesn't exist
                        continuation.resume(Result.failure(Exception("Cloudinary upload rescheduled: $rescheduleMessage")))
                    }
                })
                .dispatch() // Start the upload

            // Add a cancellation listener to cancel the Cloudinary upload if the coroutine is cancelled
            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
                Log.d(TAG, "Cloudinary upload request $requestId cancelled.")
            }
        }
    }
}

// Data class to wrap upload result (optional, can just return String or null)
// data class UploadResult(val isSuccess: Boolean, val imageUrl: String? = null, val errorMessage: String? = null)