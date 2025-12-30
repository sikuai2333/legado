package io.legado.app.help.crypto

import android.util.Base64
import androidx.annotation.Keep
import cn.hutool.core.util.HexUtil
import io.legado.app.utils.isHex
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM-NoPadding 加解密工具类
 * GCM模式提供认证加密，同时保证数据的机密性和完整性
 */
@Keep
@Suppress("unused")
object AesGcm {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // 认证标签长度（bits）
    private const val GCM_IV_LENGTH = 12 // 推荐的IV长度（bytes）

    /**
     * AES-GCM 加密
     * @param data 待加密数据
     * @param key 密钥（16/24/32字节对应AES-128/192/256）
     * @param iv 初始化向量（推荐12字节）
     * @param aad 附加认证数据（可选）
     * @return 加密后的数据（包含认证标签）
     */
    @JvmStatic
    @JvmOverloads
    fun encrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        aad?.let { cipher.updateAAD(it) }
        return cipher.doFinal(data)
    }

    /**
     * AES-GCM 加密字符串
     */
    @JvmStatic
    @JvmOverloads
    fun encrypt(
        data: String,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        return encrypt(data.toByteArray(Charsets.UTF_8), key, iv, aad)
    }

    /**
     * AES-GCM 加密并返回Base64字符串
     */
    @JvmStatic
    @JvmOverloads
    fun encryptBase64(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): String {
        return Base64.encodeToString(encrypt(data, key, iv, aad), Base64.NO_WRAP)
    }

    /**
     * AES-GCM 加密字符串并返回Base64字符串
     */
    @JvmStatic
    @JvmOverloads
    fun encryptBase64(
        data: String,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): String {
        return Base64.encodeToString(encrypt(data, key, iv, aad), Base64.NO_WRAP)
    }

    /**
     * AES-GCM 加密并返回Hex字符串
     */
    @JvmStatic
    @JvmOverloads
    fun encryptHex(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): String {
        return HexUtil.encodeHexStr(encrypt(data, key, iv, aad))
    }

    /**
     * AES-GCM 加密字符串并返回Hex字符串
     */
    @JvmStatic
    @JvmOverloads
    fun encryptHex(
        data: String,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): String {
        return HexUtil.encodeHexStr(encrypt(data, key, iv, aad))
    }

    /**
     * AES-GCM 解密
     * @param data 待解密数据（包含认证标签）
     * @param key 密钥
     * @param iv 初始化向量
     * @param aad 附加认证数据（可选，必须与加密时一致）
     * @return 解密后的数据
     */
    @JvmStatic
    @JvmOverloads
    fun decrypt(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        aad?.let { cipher.updateAAD(it) }
        return cipher.doFinal(data)
    }

    /**
     * AES-GCM 解密（自动识别Hex或Base64编码）
     */
    @JvmStatic
    @JvmOverloads
    fun decrypt(
        data: String,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        val bytes = if (data.isHex()) {
            HexUtil.decodeHex(data)
        } else {
            Base64.decode(data, Base64.NO_WRAP)
        }
        return decrypt(bytes, key, iv, aad)
    }

    /**
     * AES-GCM 解密并返回字符串
     */
    @JvmStatic
    @JvmOverloads
    fun decryptStr(
        data: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): String {
        return String(decrypt(data, key, iv, aad), Charsets.UTF_8)
    }

    /**
     * AES-GCM 解密并返回字符串（自动识别Hex或Base64编码）
     */
    @JvmStatic
    @JvmOverloads
    fun decryptStr(
        data: String,
        key: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): String {
        return String(decrypt(data, key, iv, aad), Charsets.UTF_8)
    }

    /**
     * 生成随机IV
     * @param length IV长度，默认12字节（GCM推荐）
     */
    @JvmStatic
    @JvmOverloads
    fun generateIv(length: Int = GCM_IV_LENGTH): ByteArray {
        val iv = ByteArray(length)
        SecureRandom().nextBytes(iv)
        return iv
    }

    /**
     * 生成随机密钥
     * @param length 密钥长度，16/24/32字节对应AES-128/192/256
     */
    @JvmStatic
    @JvmOverloads
    fun generateKey(length: Int = 16): ByteArray {
        require(length == 16 || length == 24 || length == 32) {
            "AES key length must be 16, 24, or 32 bytes"
        }
        val key = ByteArray(length)
        SecureRandom().nextBytes(key)
        return key
    }

    // ==================== 便捷方法（字符串参数） ====================

    /**
     * AES-GCM 加密（字符串参数）
     */
    @JvmStatic
    @JvmOverloads
    fun encrypt(
        data: String,
        key: String,
        iv: String,
        aad: String? = null
    ): ByteArray {
        return encrypt(
            data.toByteArray(Charsets.UTF_8),
            key.toByteArray(Charsets.UTF_8),
            iv.toByteArray(Charsets.UTF_8),
            aad?.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * AES-GCM 加密并返回Base64（字符串参数）
     */
    @JvmStatic
    @JvmOverloads
    fun encryptBase64(
        data: String,
        key: String,
        iv: String,
        aad: String? = null
    ): String {
        return encryptBase64(
            data.toByteArray(Charsets.UTF_8),
            key.toByteArray(Charsets.UTF_8),
            iv.toByteArray(Charsets.UTF_8),
            aad?.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * AES-GCM 加密并返回Hex（字符串参数）
     */
    @JvmStatic
    @JvmOverloads
    fun encryptHex(
        data: String,
        key: String,
        iv: String,
        aad: String? = null
    ): String {
        return encryptHex(
            data.toByteArray(Charsets.UTF_8),
            key.toByteArray(Charsets.UTF_8),
            iv.toByteArray(Charsets.UTF_8),
            aad?.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * AES-GCM 解密（字符串参数）
     */
    @JvmStatic
    @JvmOverloads
    fun decrypt(
        data: String,
        key: String,
        iv: String,
        aad: String? = null
    ): ByteArray {
        return decrypt(
            data,
            key.toByteArray(Charsets.UTF_8),
            iv.toByteArray(Charsets.UTF_8),
            aad?.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * AES-GCM 解密并返回字符串（字符串参数）
     */
    @JvmStatic
    @JvmOverloads
    fun decryptStr(
        data: String,
        key: String,
        iv: String,
        aad: String? = null
    ): String {
        return decryptStr(
            data,
            key.toByteArray(Charsets.UTF_8),
            iv.toByteArray(Charsets.UTF_8),
            aad?.toByteArray(Charsets.UTF_8)
        )
    }
}
