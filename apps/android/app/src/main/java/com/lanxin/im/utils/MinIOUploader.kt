package com.lanxin.im.utils

import android.content.Context
import android.util.Log
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.errors.MinioException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

/**
 * MinIO文件上传工具
 * 运营级别实现：真实的文件上传到MinIO对象存储
 * 
 * 服务器配置：
 * - 主服务器: 154.40.45.121:9000
 * - Bucket: lanxin-im
 */
object MinIOUploader {
    
    private const val TAG = "MinIOUploader"
    
    // MinIO配置（从BuildConfig读取，支持多环境）
    private val minioClient by lazy {
        MinioClient.builder()
            .endpoint(com.lanxin.im.BuildConfig.MINIO_ENDPOINT)
            .credentials(
                com.lanxin.im.BuildConfig.MINIO_ACCESS_KEY,
                com.lanxin.im.BuildConfig.MINIO_SECRET_KEY
            )
            .build()
    }
    
    private val BUCKET_NAME get() = com.lanxin.im.BuildConfig.MINIO_BUCKET
    
    /**
     * 上传文件到MinIO
     * @param filePath 本地文件路径
     * @param fileType 文件类型 (image/voice/video/file)
     * @return 文件的公开访问URL
     */
    suspend fun uploadFile(filePath: String, fileType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("文件不存在: $filePath"))
            }
            
            // 生成唯一文件名
            val extension = file.extension
            val uniqueFileName = "${fileType}/${UUID.randomUUID()}.$extension"
            
            // 确定Content-Type
            val contentType = getContentType(extension)
            
            // 上传到MinIO
            FileInputStream(file).use { inputStream ->
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .`object`(uniqueFileName)
                        .stream(inputStream, file.length(), -1)
                        .contentType(contentType)
                        .build()
                )
            }
            
            // 构建公开访问URL
            val fileUrl = "${MINIO_ENDPOINT}/${BUCKET_NAME}/${uniqueFileName}"
            
            Log.d(TAG, "File uploaded successfully: $fileUrl")
            Result.success(fileUrl)
            
        } catch (e: MinioException) {
            Log.e(TAG, "MinIO upload failed", e)
            Result.failure(Exception("上传失败: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 上传图片（带压缩）
     */
    suspend fun uploadImage(context: Context, imagePath: String): Result<String> {
        return try {
            // 使用ImageLoader压缩图片
            val compressedBitmap = ImageLoader.getCompressedBitmap(
                context,
                imagePath,
                maxWidth = 1920,
                maxHeight = 1080
            )
            
            if (compressedBitmap == null) {
                return Result.failure(Exception("图片压缩失败"))
            }
            
            // 保存压缩后的图片
            val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(compressedFile).use { out ->
                compressedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            compressedBitmap.recycle()
            
            // 上传压缩后的文件
            uploadFile(compressedFile.absolutePath, "images")
            
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 上传语音文件
     */
    suspend fun uploadVoice(voicePath: String): Result<String> {
        return uploadFile(voicePath, "voices")
    }
    
    /**
     * 上传视频文件
     */
    suspend fun uploadVideo(videoPath: String): Result<String> {
        return uploadFile(videoPath, "videos")
    }
    
    /**
     * 上传普通文件
     */
    suspend fun uploadDocument(documentPath: String): Result<String> {
        return uploadFile(documentPath, "documents")
    }
    
    /**
     * 根据文件扩展名获取Content-Type
     */
    private fun getContentType(extension: String): String {
        return when (extension.lowercase()) {
            // 图片
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            
            // 音频
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "amr" -> "audio/amr"
            
            // 视频
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            
            // 文档
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            
            // 压缩文件
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            
            else -> "application/octet-stream"
        }
    }
    
    /**
     * 删除MinIO上的文件
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 从URL提取对象名称
            val objectName = fileUrl.substringAfter("${BUCKET_NAME}/")
            
            minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .`object`(objectName)
                    .build()
            )
            
            Log.d(TAG, "File deleted successfully: $objectName")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            Result.failure(e)
        }
    }
}
