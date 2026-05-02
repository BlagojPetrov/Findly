package com.example.findly.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ImagePickerHelper {

    fun createImageUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "images").also { it.mkdirs() }
        val imageFile = File(imagesDir, "captured_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
}