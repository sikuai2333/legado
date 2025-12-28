package io.legado.app.help.cloud

import kotlinx.serialization.Serializable

/**
 * 云端弹窗配置数据类
 * 用于解析从服务器获取的JSON配置
 */
@Serializable
data class CloudDialogConfig(
    val title: String,
    val content: String,
    val displayCondition: String,
    val version: String,
    val buttons: List<DialogButton>,
    val latestAppVersion: String? = null,
    val latestAppVersionCode: Long? = null,
    val downloadUrl: String? = null
) {
    /**
     * 按钮配置
     */
    @Serializable
    data class DialogButton(
        val text: String,
        val action: String,
        val url: String? = null
    )

    /**
     * 显示条件枚举
     */
    object DisplayCondition {
        const val FIRST_LAUNCH = "first_launch"
        const val EVERY_LAUNCH = "every_launch"
        const val NEW_VERSION = "new_version"
    }

    /**
     * 按钮动作枚举
     */
    object ButtonAction {
        const val DOWNLOAD_UPDATE = "download_update"
        const val OPEN_BROWSER = "open_browser"
        const val CLOSE = "close"
        const val EXIT_APP = "exit_app"
    }

    /**
     * 判断是否需要显示弹窗
     * @param isFirstLaunch 是否首次启动
     * @param lastVersion 上次显示的版本号
     * @param currentAppVersionCode 当前应用版本号
     * @return 是否需要显示
     */
    fun shouldDisplay(
        isFirstLaunch: Boolean,
        lastVersion: String?,
        currentAppVersionCode: Long
    ): Boolean {
        // 如果配置了版本检查，且有新版本，优先显示更新弹窗
        latestAppVersionCode?.let { latestCode ->
            if (latestCode > currentAppVersionCode) {
                return true
            }
        }

        // 根据显示条件判断
        return when (displayCondition) {
            DisplayCondition.FIRST_LAUNCH -> isFirstLaunch
            DisplayCondition.EVERY_LAUNCH -> true
            DisplayCondition.NEW_VERSION -> version != lastVersion
            else -> false
        }
    }

    /**
     * 判断是否为版本更新弹窗
     */
    fun isUpdateDialog(currentAppVersionCode: Long): Boolean {
        return latestAppVersionCode?.let { it > currentAppVersionCode } ?: false
    }
}
