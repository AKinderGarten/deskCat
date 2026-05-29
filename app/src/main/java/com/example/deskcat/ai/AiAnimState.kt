package com.example.deskcat.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 在 ViewModel 和 OverlayView 之间共享 AI 动画帧。
 * ViewModel 写入，OverlayView 订阅播放。
 */
object AiAnimState {
    private val _frames = MutableStateFlow<List<Bitmap>?>(null)
    val frames: StateFlow<List<Bitmap>?> = _frames.asStateFlow()

    fun update(frames: List<Bitmap>?) {
        _frames.value = frames
    }
}
