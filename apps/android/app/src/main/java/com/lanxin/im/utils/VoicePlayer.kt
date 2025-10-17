package com.lanxin.im.utils

import android.media.MediaPlayer
import java.io.IOException

/**
 * 语音播放工具类（完整实现）
 */
class VoicePlayer {
    
    private var mediaPlayer: MediaPlayer? = null
    private var onCompletionListener: (() -> Unit)? = null
    
    /**
     * 播放语音
     */
    fun play(filePath: String, onCompletion: (() -> Unit)? = null): Boolean {
        return try {
            stop() // 停止之前的播放
            
            this.onCompletionListener = onCompletion
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                    release()
                    mediaPlayer = null
                }
                start()
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
    
    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stop()
    }
}

