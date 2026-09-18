package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {
    private const val BILLS_DIR = "bills"

    /**
     * Copies an image from a content URI (e.g. Gallery / Photo Picker)
     * to the app's internal private storage and returns the local file URI string.
     */
    fun saveUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val billsDir = File(context.filesDir, BILLS_DIR).apply { if (!exists()) mkdirs() }
            val fileName = "bill_${System.currentTimeMillis()}.jpg"
            val destFile = File(billsDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compresses and saves a Bitmap (from camera preview)
     * to internal storage and returns the local file URI string.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val billsDir = File(context.filesDir, BILLS_DIR).apply { if (!exists()) mkdirs() }
            val fileName = "bill_${System.currentTimeMillis()}.jpg"
            val destFile = File(billsDir, fileName)

            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a temporary FileProvider URI for taking full-resolution pictures with Camera.
     */
    fun createTempCameraUri(context: Context): Pair<Uri, File>? {
        return try {
            val cacheDir = File(context.cacheDir, "camera_photos").apply { if (!exists()) mkdirs() }
            val tempFile = File.createTempFile("camera_bill_", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            Pair(uri, tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
