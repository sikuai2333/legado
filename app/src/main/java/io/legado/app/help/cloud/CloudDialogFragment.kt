package io.legado.app.help.cloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogCloudBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.GSON
import io.legado.app.utils.dpToPx
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.system.exitProcess

/**
 * 云端弹窗对话框
 * 用于显示从服务器获取的弹窗内容
 */
class CloudDialogFragment : BaseDialogFragment(R.layout.dialog_cloud) {

    companion object {
        const val TAG = "CloudDialogFragment"
        private const val KEY_CONFIG = "config"

        fun newInstance(config: CloudDialogConfig): CloudDialogFragment {
            return CloudDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_CONFIG, GSON.toJson(config))
                }
            }
        }
    }

    private val binding by viewBinding(DialogCloudBinding::bind)
    private var config: CloudDialogConfig? = null

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        // 解析配置
        arguments?.getString(KEY_CONFIG)?.let { jsonStr ->
            config = GSON.fromJsonObject<CloudDialogConfig>(jsonStr).getOrNull()
        }

        config?.let { cfg ->
            setupDialog(cfg)
        } ?: run {
            dismissAllowingStateLoss()
        }
    }

    private fun setupDialog(config: CloudDialogConfig) {
        // 设置标题栏颜色
        binding.toolBar.setBackgroundColor(primaryColor)

        // 设置标题
        binding.toolBar.title = config.title

        // 设置内容
        var content = config.content

        // 如果是版本更新弹窗，添加版本信息
        if (config.isUpdateDialog(BuildConfig.VERSION_CODE.toLong())) {
            val currentVersion = BuildConfig.VERSION_NAME
            val latestVersion = config.latestAppVersion ?: "未知"
            val versionInfo = "当前版本：$currentVersion\n最新版本：$latestVersion\n\n"
            content = versionInfo + content
        }

        binding.tvContent.text = content

        // 设置按钮
        setupButtons(config)
    }

    private fun setupButtons(config: CloudDialogConfig) {
        binding.llButtons.removeAllViews()

        if (config.buttons.isEmpty()) {
            // 如果没有按钮，添加一个默认的关闭按钮
            addButton("关闭") {
                handleCloseAction()
            }
            return
        }

        // 添加配置的按钮
        config.buttons.forEach { buttonConfig ->
            addButton(buttonConfig.text) {
                handleButtonAction(buttonConfig, config)
            }
        }
    }

    private fun addButton(text: String, onClick: () -> Unit) {
        val button = Button(requireContext()).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                val margin = 4.dpToPx()
                setMargins(margin, 0, margin, 0)
            }
            setPadding(8.dpToPx())
            setOnClickListener {
                onClick()
            }
        }
        binding.llButtons.addView(button)
    }

    private fun handleButtonAction(
        buttonConfig: CloudDialogConfig.DialogButton,
        config: CloudDialogConfig
    ) {
        when (buttonConfig.action) {
            CloudDialogConfig.ButtonAction.DOWNLOAD_UPDATE -> {
                // 下载更新：优先使用 downloadUrl，其次使用按钮的 url
                val url = config.downloadUrl ?: buttonConfig.url
                if (!url.isNullOrBlank()) {
                    openBrowser(url)
                }
                handleCloseAction()
            }

            CloudDialogConfig.ButtonAction.OPEN_BROWSER -> {
                // 打开浏览器
                buttonConfig.url?.let { url ->
                    if (url.isNotBlank()) {
                        openBrowser(url)
                    }
                }
                handleCloseAction()
            }

            CloudDialogConfig.ButtonAction.CLOSE -> {
                // 关闭弹窗
                handleCloseAction()
            }

            CloudDialogConfig.ButtonAction.EXIT_APP -> {
                // 退出应用
                handleCloseAction()
                activity?.finish()
                exitProcess(0)
            }

            else -> {
                // 未知动作，默认关闭
                handleCloseAction()
            }
        }
    }

    private fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCloseAction() {
        // 标记弹窗已显示
        config?.let { CloudDialogManager.markDialogShown(it) }
        dismissAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保标记弹窗已显示
        config?.let { CloudDialogManager.markDialogShown(it) }
    }
}
