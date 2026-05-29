package com.example.deskcat.pack

import android.graphics.Bitmap

/**
 * pet.json 结构：
 * {
 *   "paw": {
 *     "xRatio": 0.5,   // 爪子中心相对 body 宽度的比例（0~1）
 *     "yRatio": 0.85,  // 爪子中心相对 body 高度的比例（0~1）
 *     "widthRatio": 0.45  // 爪子宽度相对 body 宽度的比例
 *   }
 * }
 * 所有字段可选，缺省值见 PawConfig.DEFAULT
 */
data class PawConfig(
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.82f,
    val widthRatio: Float = 0.45f,
) {
    companion object {
        val DEFAULT = PawConfig()
    }
}

data class PetPack(
    val body: Bitmap,
    val pawUp: Bitmap,
    val pawDown: Bitmap,
    val pawConfig: PawConfig,
)
