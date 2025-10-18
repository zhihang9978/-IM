package com.lanxin.im.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * 视频处理工具类
 * 运营级别实现：真实的视频信息提取
 */
object VideoUtils {
    
    private const val TAG = "VideoUtils"
    
    /**
     * 获取视频时长（秒）
     */
    fun getVideoDuration(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            (durationMs / 1000).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration", e)
            0
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 获取视频时长（从URI）
     */
    fun getVideoDuration(context: Context, uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            (durationMs / 1000).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration from URI", e)
            0
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 获取视频缩略图
     */
    fun getVideoThumbnail(filePath: String): android.graphics.Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video thumbnail", e)
            null
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 获取视频宽度
     */
    fun getVideoWidth(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            widthStr?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video width", e)
            0
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 获取视频高度
     */
    fun getVideoHeight(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            heightStr?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video height", e)
            0
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 获取视频文件大小（字节）
     */
    fun getVideoFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video file size", e)
            0L
        }
    }
    
    /**
     * 格式化视频时长显示
     * @param seconds 秒数
     * @return 格式化的时长字符串 (如: "01:23" 或 "1:23:45")
     */
    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    /**
     * 格式化文件大小显示
     * @param bytes 字节数
     * @return 格式化的大小字符串 (如: "1.5 MB")
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
