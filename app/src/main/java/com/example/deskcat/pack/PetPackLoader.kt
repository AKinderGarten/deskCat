package com.example.deskcat.pack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

object PetPackLoader {

    private const val PACK_DIR = "pet_pack"

    fun getPackDir(context: Context): File =
        File(context.filesDir, PACK_DIR)

    /**
     * 从 zip URI 解压资源包到内部存储，返回解压目录路径。
     * zip 内需包含：body.png, paw_up.png, paw_down.png
     * 可选：pet.json（爪子位置配置）
     */
    fun importFromZip(context: Context, zipUri: Uri): String? {
        val outDir = getPackDir(context)
        outDir.deleteRecursively()
        outDir.mkdirs()

        return runCatching {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = File(entry.name).name // 只取文件名，忽略目录层级
                        if (!entry.isDirectory && name.isNotBlank()) {
                            File(outDir, name).outputStream().use { out ->
                                zip.copyTo(out)
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            outDir.absolutePath
        }.getOrNull()
    }

    /**
     * 从内部存储目录加载资源包，返回 PetPack 或 null（文件不完整时）
     */
    fun loadFromDir(context: Context, dirPath: String? = null): PetPack? {
        val dir = if (dirPath != null) File(dirPath) else getPackDir(context)
        if (!dir.exists()) return null

        val bodyFile = File(dir, "body.png")
        val pawUpFile = File(dir, "paw_up.png")
        val pawDownFile = File(dir, "paw_down.png")

        if (!bodyFile.exists() || !pawUpFile.exists() || !pawDownFile.exists()) return null

        val body = BitmapFactory.decodeFile(bodyFile.absolutePath) ?: return null
        val pawUp = BitmapFactory.decodeFile(pawUpFile.absolutePath) ?: return null
        val pawDown = BitmapFactory.decodeFile(pawDownFile.absolutePath) ?: return null

        val pawConfig = runCatching {
            val json = JSONObject(File(dir, "pet.json").readText())
            val paw = json.optJSONObject("paw") ?: return@runCatching PawConfig.DEFAULT
            PawConfig(
                xRatio = paw.optDouble("xRatio", 0.5).toFloat(),
                yRatio = paw.optDouble("yRatio", 0.82).toFloat(),
                widthRatio = paw.optDouble("widthRatio", 0.45).toFloat(),
            )
        }.getOrElse { PawConfig.DEFAULT }

        return PetPack(body = body, pawUp = pawUp, pawDown = pawDown, pawConfig = pawConfig)
    }

    fun clearPack(context: Context) {
        getPackDir(context).deleteRecursively()
    }

    /**
     * 直接从三个独立文件 URI 导入（不需要打包成 zip）
     */
    fun importFromFiles(
        context: Context,
        bodyUri: Uri,
        pawUpUri: Uri,
        pawDownUri: Uri,
        pawConfig: PawConfig = PawConfig.DEFAULT,
    ): String? {
        val outDir = getPackDir(context)
        outDir.deleteRecursively()
        outDir.mkdirs()

        return runCatching {
            fun copyUri(uri: Uri, name: String) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(outDir, name).outputStream().use { input.copyTo(it) }
                }
            }
            copyUri(bodyUri, "body.png")
            copyUri(pawUpUri, "paw_up.png")
            copyUri(pawDownUri, "paw_down.png")

            // 写入 pet.json
            val json = JSONObject().apply {
                put("paw", JSONObject().apply {
                    put("xRatio", pawConfig.xRatio.toDouble())
                    put("yRatio", pawConfig.yRatio.toDouble())
                    put("widthRatio", pawConfig.widthRatio.toDouble())
                })
            }
            File(outDir, "pet.json").writeText(json.toString())

            outDir.absolutePath
        }.getOrNull()
    }
}
