package io.legado.app.help.source

import android.util.Base64
import io.legado.app.data.entities.BookSource
import io.legado.app.help.crypto.AesGcm
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.init.appCtx

/**
 * 加密书源管理器
 * 用于从 assets 中读取加密的书源文件并解密导入
 * 解密在内存中完成，不会落地到用户可访问的文件夹
 */
object EncryptedBookSourceManager {

    private const val ENCRYPTED_SOURCE_FILE = "encsy"

    // AES-GCM 解密密钥和IV（Base64编码，与服务器加密时使用的相同）
    private const val AES_KEY_BASE64 = "R4LmwbWucdfsMDrHi7rGwQ=="
    private val aesKey: ByteArray by lazy { Base64.decode(AES_KEY_BASE64, Base64.NO_WRAP) }
    private val aesIv: ByteArray by lazy { Base64.decode(AES_KEY_BASE64, Base64.NO_WRAP) }

    /**
     * 从 assets 读取并解密书源
     * @return 解密后的书源列表，失败返回空列表
     */
    suspend fun loadEncryptedSources(): List<BookSource> {
        return withContext(Dispatchers.IO) {
            try {
                // 从 assets 读取加密数据
                val encryptedData = appCtx.assets.open(ENCRYPTED_SOURCE_FILE).use { inputStream ->
                    inputStream.bufferedReader().readText()
                }

                // 解密数据
                val jsonStr = decryptSourceData(encryptedData) ?: return@withContext emptyList()

                // 解析 JSON 为书源列表
                GSON.fromJsonArray<BookSource>(jsonStr).getOrNull() ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * 解密书源数据
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的JSON字符串，失败返回null
     */
    private fun decryptSourceData(encryptedData: String): String? {
        return try {
            AesGcm.decryptStr(encryptedData, aesKey, aesIv, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导入加密书源到数据库
     * @return 导入的书源数量
     */
    suspend fun importEncryptedSources(): Int {
        val sources = loadEncryptedSources()
        if (sources.isEmpty()) {
            return 0
        }

        // 使用 SourceHelp 导入书源
        SourceHelp.insertBookSource(*sources.toTypedArray())
        return sources.size
    }
}
