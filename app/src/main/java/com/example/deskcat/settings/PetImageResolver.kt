package com.example.deskcat.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.deskcat.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object PetImageResolver {

    fun decodeBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        val uri = Uri.parse(uriString)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull()
    }

    suspend fun decodeAndRemoveBackground(context: Context, uriString: String?): Bitmap? {
        val original = decodeBitmap(context, uriString) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { removeBackgroundViaApi(context, uriString!!, original) }.getOrElse { original }
        }
    }

    private fun removeBackgroundViaApi(context: Context, uriString: String, fallback: Bitmap): Bitmap {
        val uri = Uri.parse(uriString)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return fallback
        val imageBytes = inputStream.use { it.readBytes() }

        val boundary = "----FormBoundary${System.currentTimeMillis()}"
        val url = java.net.URL("https://api.remove.bg/v1.0/removebg")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("X-Api-Key", ApiConfig.REMOVE_BG_API_KEY)
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000

        connection.outputStream.use { out ->
            val writer = out.bufferedWriter()
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"image_file\"; filename=\"pet.png\"\r\n")
            writer.write("Content-Type: image/png\r\n\r\n")
            writer.flush()
            out.write(imageBytes)
            out.flush()
            writer.write("\r\n--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"size\"\r\n\r\n")
            writer.write("auto\r\n")
            writer.write("--$boundary--\r\n")
            writer.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode != 200) return fallback

        val resultBytes = connection.inputStream.use { it.readBytes() }
        return BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size) ?: fallback
    }
}
