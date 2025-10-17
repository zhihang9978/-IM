package com.lanxin.im.utils

import android.content.Context
import android.net.Uri
import com.lanxin.im.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * 文件上传助手
 * 通过后端API上传文件到MinIO
 */
object FileUploadHelper {
    
    // MinIO配置
    private const val MINIO_URL = "http://154.40.45.121:9000"
    
    /**
     * 上传文件到MinIO（通过后端API）
     * @param context Context
     * @param uri 文件URI
     * @param fileType 文件类型 (image/voice/video/file)
     * @return 上传后的文件URL，失败返回null
     */
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        fileType: String
    ): String? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            // 1. 从URI复制文件到临时文件
            tempFile = uriToFile(context, uri, fileType)
            
            // 2. 获取上传凭证
            val fileName = tempFile.name
            val uploadTokenResponse = RetrofitClient.apiService.getUploadToken(
                fileType = fileType,
                fileName = fileName
            )
            
            if (uploadTokenResponse.code != 0 || uploadTokenResponse.data == null) {
                return@withContext null
            }
            
            val tokenData = uploadTokenResponse.data
            val bucket = tokenData.bucket
            val key = tokenData.key
            
            // 3. 直接上传到MinIO
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$MINIO_URL/$bucket/$key")
                .put(tempFile.asRequestBody(getMimeType(fileType).toMediaTypeOrNull()))
                .addHeader("Content-Type", getMimeType(fileType))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                // 4. 回调通知后端
                val fileUrl = "$MINIO_URL/$bucket/$key"
                RetrofitClient.apiService.uploadCallback(
                    com.lanxin.im.data.remote.UploadCallbackRequest(
                        key = key,
                        url = fileUrl,
                        size = tempFile.length(),
                        content_type = getMimeType(fileType)
                    )
                )
                
                return@withContext fileUrl
            } else {
                return@withContext null
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            // 清理临时文件
            tempFile?.delete()
        }
    }
    
    /**
     * 从URI复制文件到临时文件
     */
    private fun uriToFile(context: Context, uri: Uri, fileType: String): File {
        val extension = when (fileType) {
            "image" -> "jpg"
            "voice" -> "m4a"
            "video" -> "mp4"
            else -> "dat"
        }
        
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
    
    /**
     * 获取MIME类型
     */
    private fun getMimeType(fileType: String): String {
        return when (fileType) {
            "image" -> "image/jpeg"
            "voice" -> "audio/m4a"
            "video" -> "video/mp4"
            "file" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }
}
