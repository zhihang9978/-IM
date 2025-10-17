package com.lanxin.im.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

/**
 * 视频压缩工具
 * 参考：WildFireChat视频压缩策略 (Apache 2.0)
 * 
 * 压缩策略：
 * - 最大分辨率：1280x720（720p）
 * - 码率：2Mbps
 * - 使用MediaCodec硬件编码
 * 
 * 注意：完整的视频压缩需要使用第三方库（如ffmpeg、MediaCodec）
 * 当前实现为简化版本，仅提供接口和基本验证
 */
class VideoCompressor(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoCompressor"
        private const val MAX_WIDTH = 1280
        private const val MAX_HEIGHT = 720
        private const val MAX_BITRATE = 2000000 // 2Mbps
        private const val MAX_DURATION = 300 // 5分钟
    }
    
    /**
     * 压缩视频
     * 
     * 简化实现：检查视频元数据，如果已经符合要求则跳过压缩
     * 完整实现需要集成ffmpeg或MediaCodec进行实际压缩
     * 
     * @param uri 视频URI
     * @return 压缩后的文件（或原文件如果不需要压缩）
     */
    fun compress(uri: Uri): File {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            // 获取视频元数据
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            
            // 检查是否需要压缩
            val needsCompression = width > MAX_WIDTH || 
                                   height > MAX_HEIGHT || 
                                   bitrate > MAX_BITRATE ||
                                   duration > MAX_DURATION * 1000
            
            if (!needsCompression) {
                // 不需要压缩，返回原文件
                return File(getRealPathFromURI(uri))
            }
            
            // ✅ 完整实现建议：
            // 方案1: 使用MediaCodec（Android原生，复杂）
            // 方案2: 集成FFmpeg（功能强大，包体积大）
            // 方案3: 使用Transcoder库（推荐）：https://github.com/natario1/Transcoder
            // 当前简化版本：检查元数据，符合标准则跳过压缩
            
            return File(getRealPathFromURI(uri))
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Video compression failed", e)
            // 压缩失败，返回原文件
            return File(getRealPathFromURI(uri))
        } finally {
            retriever.release()
        }
    }
    
    /**
     * 获取URI的真实文件路径
     */
    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val columnIndex = it.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
            if (it.moveToFirst() && columnIndex >= 0) {
                it.getString(columnIndex)
            } else {
                uri.path ?: ""
            }
        } ?: uri.path ?: ""
    }
    
    /**
     * 获取视频信息
     */
    fun getVideoInfo(uri: Uri): VideoInfo? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            
            return VideoInfo(width, height, duration, bitrate)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get video info", e)
            return null
        } finally {
            retriever.release()
        }
    }
}

/**
 * 视频信息数据类
 */
data class VideoInfo(
    val width: Int,
    val height: Int,
    val duration: Long, // 毫秒
    val bitrate: Int    // 比特率
)
