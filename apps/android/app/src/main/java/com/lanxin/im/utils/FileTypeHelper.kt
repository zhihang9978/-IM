package com.lanxin.im.utils

import com.lanxin.im.R

/**
 * 文件类型辅助工具类
 * 根据文件名扩展名返回对应的文件类型图标
 * 
 * 参考：WildFireChat android-chat-master (Apache 2.0)
 * 适配：蓝信IM
 */
object FileTypeHelper {
    
    /**
     * 根据文件名获取对应的文件类型图标资源ID
     * @param fileName 文件名
     * @return 文件类型图标资源ID
     */
    fun getFileTypeIcon(fileName: String): Int {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        
        return when (extension) {
            // PDF
            "pdf" -> R.mipmap.ic_file_type_pdf
            
            // Word
            "doc", "docx", "wps" -> R.mipmap.ic_file_type_word
            
            // Excel
            "xls", "xlsx", "et" -> R.mipmap.ic_file_type_excel
            
            // PowerPoint
            "ppt", "pptx", "dps" -> R.mipmap.ic_file_type_ppt
            
            // 图片
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> R.mipmap.ic_file_type_image
            
            // 视频
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "3gp" -> R.mipmap.ic_file_type_video
            
            // 音频
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma" -> R.mipmap.ic_file_type_audio
            
            // 压缩包
            "zip", "rar", "7z", "tar", "gz", "bz2" -> R.mipmap.ic_file_type_zip
            
            // 未知类型
            else -> R.mipmap.ic_file_type_unknown
        }
    }
    
    /**
     * 格式化文件大小
     * @param bytes 文件大小（字节）
     * @return 格式化后的文件大小字符串（如 "1.5 MB"）
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return when {
            size < 10 -> String.format("%.1f %s", size, units[unitIndex])
            else -> String.format("%.0f %s", size, units[unitIndex])
        }
    }
}

