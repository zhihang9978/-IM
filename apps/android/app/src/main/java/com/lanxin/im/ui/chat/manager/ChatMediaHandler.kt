package com.lanxin.im.ui.chat.manager

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.lanxin.im.data.model.Message
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.utils.PermissionHelper
import com.lanxin.im.utils.VideoCompressor
import com.lanxin.im.utils.VoicePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 聊天多媒体处理器
 * 负责处理图片、视频、文件、语音的选择、压缩、上传
 * 
 * 参考：WildFireChat MediaMessageContent (Apache 2.0)
 */
class ChatMediaHandler(
    private val activity: AppCompatActivity,
    private val onImageSelected: (String) -> Unit,
    private val onVideoSelected: (String) -> Unit,
    private val onFileSelected: (String, String, Long) -> Unit
) {
    
    private var currentPhotoUri: Uri? = null
    private var currentVideoUri: Uri? = null
    
    private val voicePlayer = VoicePlayer()
    
    // Activity result launchers
    private val pickImageLauncher: ActivityResultLauncher<String>
    private val takePictureLauncher: ActivityResultLauncher<Uri?>
    private val recordVideoLauncher: ActivityResultLauncher<Uri?>
    private val pickFileLauncher: ActivityResultLauncher<String>
    
    init {
        pickImageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { handleImageSelected(it) }
        }
        
        takePictureLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                currentPhotoUri?.let { handleImageSelected(it) }
            }
        }
        
        recordVideoLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success ->
            if (success) {
                currentVideoUri?.let { handleVideoSelected(it) }
            }
        }
        
        pickFileLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { handleFileSelected(it) }
        }
    }
    
    // Public methods
    
    fun selectImageFromAlbum() {
        pickImageLauncher.launch("image/*")
    }
    
    fun takePicture() {
        if (PermissionHelper.hasCameraPermission(activity)) {
            val photoFile = File(
                activity.getExternalFilesDir(null),
                "photo_${System.currentTimeMillis()}.jpg"
            )
            currentPhotoUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri)
        } else {
            PermissionHelper.requestCameraPermission(activity)
        }
    }
    
    fun recordVideo() {
        if (PermissionHelper.hasCameraPermission(activity)) {
            val videoFile = File(
                activity.getExternalFilesDir(null),
                "video_${System.currentTimeMillis()}.mp4"
            )
            currentVideoUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                videoFile
            )
            recordVideoLauncher.launch(currentVideoUri)
        } else {
            PermissionHelper.requestCameraPermission(activity)
        }
    }
    
    fun selectFile() {
        pickFileLauncher.launch("*/*")
    }
    
    fun playVoiceMessage(message: Message) {
        message.fileUrl?.let { url ->
            voicePlayer.play(url)
        } ?: run {
            Toast.makeText(activity, "语音文件不存在", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun previewImage(message: Message) {
        message.fileUrl?.let { url ->
            // Open image preview activity
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "image/*")
            activity.startActivity(intent)
        }
    }
    
    fun playVideo(message: Message) {
        message.fileUrl?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "video/*")
            activity.startActivity(intent)
        }
    }
    
    fun openFile(message: Message) {
        message.fileUrl?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = getMimeType(message.content)
            intent.setDataAndType(Uri.parse(url), mimeType)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "无法打开文件", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Private methods
    
    private fun handleImageSelected(uri: Uri) {
        activity.lifecycleScope.launch {
            try {
                val compressedFile = compressImage(uri)
                onImageSelected(compressedFile.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(activity, "图片处理失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleVideoSelected(uri: Uri) {
        activity.lifecycleScope.launch {
            try {
                compressAndSendVideo(uri)
            } catch (e: Exception) {
                Toast.makeText(activity, "视频处理失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleFileSelected(uri: Uri) {
        activity.lifecycleScope.launch {
            try {
                val fileName = getFileName(uri)
                val fileSize = getFileSize(uri)
                
                // Copy file to cache directory
                val cacheFile = File(activity.cacheDir, fileName)
                activity.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                onFileSelected(cacheFile.absolutePath, fileName, fileSize)
            } catch (e: Exception) {
                Toast.makeText(activity, "文件处理失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun compressImage(uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = activity.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // Calculate target size (max 1080px)
        val maxSize = 1080
        val width = bitmap.width
        val height = bitmap.height
        
        val scaledBitmap = if (width > maxSize || height > maxSize) {
            val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
            val scaledWidth = (width * scale).toInt()
            val scaledHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap
        }
        
        // Compress to JPEG
        val outputFile = File(activity.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        bitmap.recycle()
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        
        outputFile
    }
    
    private suspend fun compressAndSendVideo(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            // Compress video using VideoCompressor instance
            val compressor = VideoCompressor(activity)
            val compressedFile = compressor.compress(uri)
            
            withContext(Dispatchers.Main) {
                onVideoSelected(compressedFile.absolutePath)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "视频压缩失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        val cursor = activity.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            it.moveToFirst()
            it.getString(columnIndex)
        }
    }
    
    private fun getFileName(uri: Uri): String {
        var fileName = "file_${System.currentTimeMillis()}"
        activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
    
    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }
        return fileSize
    }
    
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            else -> "*/*"
        }
    }
    
    fun cleanup() {
        voicePlayer.release()
    }
}
