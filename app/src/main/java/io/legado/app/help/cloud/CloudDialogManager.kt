package io.legado.app.help.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import io.legado.app.BuildConfig
import io.legado.app.help.crypto.AesGcm
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.init.appCtx

/**
 * 云端弹窗管理器
 * 负责从服务器获取弹窗配置并管理显示状态
 */
object CloudDialogManager {

    private const val CLOUD_DIALOG_URL = "https://xs.zhigeyun.com/tc"
    private const val PREFS_NAME = "cloud_dialog"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_LAST_VERSION = "last_version"
    private const val KEY_LAST_UPDATE_VERSION = "last_update_version"

    // AES-GCM 解密密钥和IV（Base64编码）
    private const val AES_KEY_BASE64 = "R4LmwbWucdfsMDrHi7rGwQ=="
    private val aesKey: ByteArray by lazy { Base64.decode(AES_KEY_BASE64, Base64.NO_WRAP) }
    private val aesIv: ByteArray by lazy { Base64.decode(AES_KEY_BASE64, Base64.NO_WRAP) }

    private val prefs: SharedPreferences by lazy {
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 进程级别的标记，防止Activity重建时重复显示
     * 每次应用进程启动时重置为false
     */
    @Volatile
    private var hasShownInCurrentProcess = false

    /**
     * 从云端获取弹窗配置
     * @return CloudDialogConfig 或 null（如果获取失败）
     */
    suspend fun fetchCloudDialogConfig(): CloudDialogConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val response = okHttpClient.newCallStrResponse {
                    url(CLOUD_DIALOG_URL)
                }

                if (response.isSuccessful()) {
                    val encryptedData = response.body()
                    if (!encryptedData.isNullOrBlank()) {
                        // 使用 AES-GCM 解密
                        val jsonStr = decryptConfig(encryptedData)
                        if (jsonStr != null) {
                            GSON.fromJsonObject<CloudDialogConfig>(jsonStr).getOrNull()
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 解密云端配置
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的JSON字符串，失败返回null
     */
    private fun decryptConfig(encryptedData: String): String? {
        return try {
            AesGcm.decryptStr(encryptedData, aesKey, aesIv, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 检查是否需要显示云端弹窗
     * @return CloudDialogConfig 如果需要显示，否则返回 null
     */
    suspend fun checkAndGetDialog(): CloudDialogConfig? {
        // 进程级别检查：如果本次进程已经显示过，直接返回null
        if (hasShownInCurrentProcess) {
            return null
        }

        val config = fetchCloudDialogConfig() ?: return null

        val isFirstLaunch = isFirstLaunch()
        val lastVersion = getLastVersion()
        val currentAppVersionCode = BuildConfig.VERSION_CODE.toLong()

        // 判断是否需要显示
        if (config.shouldDisplay(isFirstLaunch, lastVersion, currentAppVersionCode)) {
            return config
        }

        return null
    }

    /**
     * 标记弹窗已显示
     * @param config 已显示的弹窗配置
     */
    fun markDialogShown(config: CloudDialogConfig) {
        // 标记进程级别已显示
        hasShownInCurrentProcess = true

        prefs.edit {
            // 标记不再是首次启动
            putBoolean(KEY_FIRST_LAUNCH, false)

            // 保存当前配置版本号
            putString(KEY_LAST_VERSION, config.version)

            // 如果是更新弹窗，保存更新版本号
            if (config.isUpdateDialog(BuildConfig.VERSION_CODE.toLong())) {
                config.latestAppVersion?.let {
                    putString(KEY_LAST_UPDATE_VERSION, it)
                }
            }
        }
    }

    /**
     * 判断是否为首次启动
     */
    private fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 获取上次显示的配置版本号
     */
    private fun getLastVersion(): String? {
        return prefs.getString(KEY_LAST_VERSION, null)
    }

    /**
     * 获取上次显示的更新版本号
     */
    fun getLastUpdateVersion(): String? {
        return prefs.getString(KEY_LAST_UPDATE_VERSION, null)
    }

    /**
     * 重置首次启动标记（用于测试）
     */
    fun resetFirstLaunch() {
        prefs.edit {
            putBoolean(KEY_FIRST_LAUNCH, true)
        }
    }

    /**
     * 重置进程级别标记（用于测试）
     * 注意：这只会重置内存标记，不会影响SharedPreferences
     */
    fun resetProcessFlag() {
        hasShownInCurrentProcess = false
    }

    /**
     * 清除所有云端弹窗数据（用于测试）
     */
    fun clearAll() {
        hasShownInCurrentProcess = false
        prefs.edit {
            clear()
        }
    }
}
