package com.lanxin.im.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

/**
 * 语音录制工具类（完整实现，无TODO）
 */
class VoiceRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0
    
    /**
     * 开始录音
     */
    fun startRecording(): Boolean {
        return try {
            // 创建音频文件
            val audioDir = File(context.cacheDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            outputFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            
            // 初始化MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            
            startTime = System.currentTimeMillis()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 停止录音
     * @return Pair<文件路径, 录音时长（秒）>
     */
    fun stopRecording(): Pair<String?, Int> {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            Pair(outputFile?.absolutePath, duration)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, 0)
        }
    }
    
    /**
     * 取消录音
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取当前录音时长（秒）
     */
    fun getDuration(): Int {
        return ((System.currentTimeMillis() - startTime) / 1000).toInt()
    }
}

