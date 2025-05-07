package com.codewithfk.chatooz_android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ImagePickerHelper(private val context: Context) {
    private var onImageSelected: ((Uri) -> Unit)? = null
    private var onError: ((Exception) -> Unit)? = null

    private val pickImageLauncher = (context as AppCompatActivity).registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                result.data?.data?.let { uri ->
                    onImageSelected?.invoke(uri)
                } ?: run {
                    onError?.invoke(Exception("No image was selected"))
                }
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    fun pickImage(
        onSelected: (Uri) -> Unit,
        onError: (Exception) -> Unit = { _ -> }
    ) {
        this.onImageSelected = onSelected
        this.onError = onError
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }
}