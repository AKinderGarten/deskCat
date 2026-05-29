package com.example.deskcat.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import com.example.deskcat.config.ApiConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 调用豆包多模态 API，根据宠物图片生成逐帧静态动画帧列表。
 * 返回的每个 Bitmap 是一帧，调用方循环播放即可。
 */
object DoubaoAnimGenerator {

    private const val FRAME_COUNT = 6
    private const val FRAME_SIZE = 256

    private val SYSTEM_PROMPT = """
你是一个专业的像素动画设计师。用户会提供一张宠物图片，你需要为它设计一段循环动画。

请严格按照以下 JSON 格式返回，不要输出任何其他内容：
{
  "frames": [
    {
      "index": 0,
      "description": "第0帧：宠物静止，四肢自然放松，尾巴水平",
      "body_offset_y": 0,
      "tail_angle": 0,
      "ear_twitch": false,
      "paw_raise": "none"
    }
  ],
  "fps": 8,
  "loop": true
}

字段说明：
- body_offset_y: 身体垂直偏移像素（-4 到 4），模拟呼吸/弹跳
- tail_angle: 尾巴摆动角度（-30 到 30 度）
- ear_twitch: 耳朵是否抖动
- paw_raise: 抬爪状态，可选 "none" / "left" / "right" / "both"

要求：
1. 共生成 ${FRAME_COUNT} 帧，index 从 0 到 ${FRAME_COUNT - 1}
2. 动画要流畅自然，首尾可衔接循环
3. 根据宠物种类选择合适的动作风格（猫咪慵懒、狗狗活泼、兔子轻柔）
""".trimIndent()

    data class AnimFrame(
        val index: Int,
        val bodyOffsetY: Int,
        val tailAngle: Float,
        val earTwitch: Boolean,
        val pawRaise: String,
        val description: String,
    )

    data class AnimSpec(
        val frames: List<AnimFrame>,
        val fps: Int,
        val loop: Boolean,
    )

    /**
     * 根据宠物图片调用豆包 API 获取动画规格，再将规格渲染为 Bitmap 帧列表。
     * 在 IO 线程调用。
     */
    fun generate(sourceBitmap: Bitmap): List<Bitmap> {
        val spec = requestAnimSpec(sourceBitmap) ?: return emptyList()
        return renderFrames(sourceBitmap, spec)
    }

    private fun requestAnimSpec(bitmap: Bitmap): AnimSpec? {
        val base64Image = bitmapToBase64(bitmap)
        val requestBody = buildRequestJson(base64Image)

        val url = URL("${ApiConfig.DOUBAO_BASE_URL}/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${ApiConfig.DOUBAO_API_KEY}")
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000

            conn.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val responseText = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
                return null
            }

            parseAnimSpec(responseText)
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun buildRequestJson(base64Image: String): String {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/png;base64,$base64Image")
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", "请根据这张宠物图片，生成一段 ${FRAME_COUNT} 帧的循环动画规格。")
                    })
                })
            })
        }

        return JSONObject().apply {
            put("model", ApiConfig.DOUBAO_MODEL)
            put("messages", messages)
            put("max_tokens", 1024)
            put("temperature", 0.7)
        }.toString()
    }

    private fun parseAnimSpec(responseJson: String): AnimSpec? {
        return try {
            val root = JSONObject(responseJson)
            val content = root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            // 从 content 中提取 JSON 块（模型可能包裹在 ```json ... ``` 中）
            val jsonText = extractJson(content) ?: return null
            val parsed = JSONObject(jsonText)
            val framesArray = parsed.getJSONArray("frames")
            val fps = parsed.optInt("fps", 8)
            val loop = parsed.optBoolean("loop", true)

            val frames = (0 until framesArray.length()).map { i ->
                val f = framesArray.getJSONObject(i)
                AnimFrame(
                    index = f.optInt("index", i),
                    bodyOffsetY = f.optInt("body_offset_y", 0),
                    tailAngle = f.optDouble("tail_angle", 0.0).toFloat(),
                    earTwitch = f.optBoolean("ear_twitch", false),
                    pawRaise = f.optString("paw_raise", "none"),
                    description = f.optString("description", ""),
                )
            }

            AnimSpec(frames = frames, fps = fps, loop = loop)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    /**
     * 将动画规格渲染为 Bitmap 帧列表。
     * 每帧在原图基础上应用 body_offset_y 偏移，模拟弹跳/呼吸感。
     * 尾巴/耳朵/爪子的视觉变化通过偏移和缩放近似表达，
     * 不依赖骨骼绑定，保持实现简单。
     */
    private fun renderFrames(source: Bitmap, spec: AnimSpec): List<Bitmap> {
        val scaled = Bitmap.createScaledBitmap(source, FRAME_SIZE, FRAME_SIZE, true)
        return spec.frames.map { frame ->
            val output = Bitmap.createBitmap(FRAME_SIZE, FRAME_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.TRANSPARENT)

            val offsetY = frame.bodyOffsetY.coerceIn(-8, 8).toFloat()
            // 爪子抬起时身体轻微上移
            val pawLift = when (frame.pawRaise) {
                "both" -> -2f
                "left", "right" -> -1f
                else -> 0f
            }
            canvas.save()
            canvas.translate(0f, offsetY + pawLift)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            canvas.restore()

            output
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
            val ratio = minOf(512f / bitmap.width, 512f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true,
            )
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
