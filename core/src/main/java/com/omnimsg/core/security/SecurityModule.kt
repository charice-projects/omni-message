// 📁 core/security/EncryptionManager.kt
package com.omnimsg.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "omnimessage_master_key"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_SIZE = 256
    }
    
    sealed class EncryptionResult {
        data class Success(val data: ByteArray) : EncryptionResult()
        data class Error(val message: String) : EncryptionResult()
    }
    
    data class EncryptionMetadata(
        val algorithm: String,
        val keyAlias: String,
        val timestamp: Long,
        val iv: ByteArray? = null
    )
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    private lateinit var aead: Aead
    private var isInitialized = false
    
    /**
     * 初始化加密管理器
     */
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 初始化Tink
                AeadConfig.register()
                
                // 创建或获取密钥
                val keysetHandle = AndroidKeysetManager.Builder()
                    .withSharedPref(context, "omnimessage_keyset", "master_key")
                    .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
                    .withMasterKeyUri("android-keystore://omnimessage_master_key")
                    .build()
                    .keysetHandle
                
                aead = keysetHandle.getPrimitive(Aead::class.java)
                isInitialized = true
                
                logger.i("EncryptionManager", "加密管理器初始化成功")
                true
            } catch (e: Exception) {
                logger.e("EncryptionManager", "加密管理器初始化失败", e)
                false
            }
        }
    }
    
    /**
     * 加密数据
     */
    suspend fun encrypt(data: ByteArray, associatedData: ByteArray? = null): EncryptionResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized && !initialize()) {
                    return@withContext EncryptionResult.Error("加密管理器未初始化")
                }
                
                val encryptedData = aead.encrypt(data, associatedData)
                EncryptionResult.Success(encryptedData)
            } catch (e: Exception) {
                logger.e("EncryptionManager", "加密数据失败", e)
                EncryptionResult.Error("加密失败: ${e.message}")
            }
        }
    }
    
    /**
     * 解密数据
     */
    suspend fun decrypt(encryptedData: ByteArray, associatedData: ByteArray? = null): EncryptionResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!isInitialized && !initialize()) {
                    return@withContext EncryptionResult.Error("加密管理器未初始化")
                }
                
                val decryptedData = aead.decrypt(encryptedData, associatedData)
                EncryptionResult.Success(decryptedData)
            } catch (e: Exception) {
                logger.e("EncryptionManager", "解密数据失败", e)
                EncryptionResult.Error("解密失败: ${e.message}")
            }
        }
    }
    
    /**
     * 加密字符串
     */
    suspend fun encryptString(text: String): String {
        return try {
            val result = encrypt(text.toByteArray(StandardCharsets.UTF_8))
            when (result) {
                is EncryptionResult.Success -> result.data.toHexString()
                is EncryptionResult.Error -> throw IllegalStateException(result.message)
            }
        } catch (e: Exception) {
            logger.e("EncryptionManager", "加密字符串失败", e)
            throw e
        }
    }
    
    /**
     * 解密字符串
     */
    suspend fun decryptString(encryptedHex: String): String {
        return try {
            val encryptedData = encryptedHex.hexToByteArray()
            val result = decrypt(encryptedData)
            when (result) {
                is EncryptionResult.Success -> String(result.data, StandardCharsets.UTF_8)
                is EncryptionResult.Error -> throw IllegalStateException(result.message)
            }
        } catch (e: Exception) {
            logger.e("EncryptionManager", "解密字符串失败", e)
            throw e
        }
    }
    
    /**
     * 加密文件
     */
    suspend fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                
                val encryptedFile = EncryptedFile.Builder(
                    outputFile,
                    context,
                    masterKeyAlias,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                
                FileInputStream(inputFile).use { inputStream ->
                    encryptedFile.openFileOutput().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                true
            } catch (e: Exception) {
                logger.e("EncryptionManager", "加密文件失败", e)
                false
            }
        }
    }
    
    /**
     * 解密文件
     */
    suspend fun decryptFile(inputFile: File, outputFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                
                val encryptedFile = EncryptedFile.Builder(
                    inputFile,
                    context,
                    masterKeyAlias,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                
                encryptedFile.openFileInput().use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                true
            } catch (e: Exception) {
                logger.e("EncryptionManager", "解密文件失败", e)
                false
            }
        }
    }
    
    /**
     * 生成加密密钥
     */
    fun generateKey(keyAlias: String = KEY_ALIAS): Boolean {
        return try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(false)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            true
        } catch (e: Exception) {
            logger.e("EncryptionManager", "生成密钥失败", e)
            false
        }
    }
    
    /**
     * 检查密钥是否存在
     */
    fun hasKey(keyAlias: String = KEY_ALIAS): Boolean {
        return try {
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            logger.e("EncryptionManager", "检查密钥失败", e)
            false
        }
    }
    
    /**
     * 删除密钥
     */
    fun deleteKey(keyAlias: String = KEY_ALIAS): Boolean {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.e("EncryptionManager", "删除密钥失败", e)
            false
        }
    }
    
    /**
     * 生成安全的随机数
     */
    fun generateRandomBytes(size: Int): ByteArray {
        return ByteArray(size).apply {
            java.security.SecureRandom().nextBytes(this)
        }
    }
    
    /**
     * 生成初始化向量
     */
    fun generateIV(): ByteArray {
        return generateRandomBytes(12) // GCM推荐12字节IV
    }
    
    /**
     * 使用自定义密钥加密
     */
    suspend fun encryptWithKey(
        data: ByteArray,
        secretKey: SecretKey,
        iv: ByteArray = generateIV()
    ): EncryptionResult {
        return withContext(Dispatchers.IO) {
            try {
                val cipher = Cipher.getInstance(AES_MODE)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
                
                val encryptedData = cipher.doFinal(data)
                val result = iv + encryptedData // IV + 密文
                
                EncryptionResult.Success(result)
            } catch (e: Exception) {
                logger.e("EncryptionManager", "使用自定义密钥加密失败", e)
                EncryptionResult.Error("加密失败: ${e.message}")
            }
        }
    }
    
    /**
     * 使用自定义密钥解密
     */
    suspend fun decryptWithKey(
        encryptedData: ByteArray,
        secretKey: SecretKey
    ): EncryptionResult {
        return withContext(Dispatchers.IO) {
            try {
                // 提取IV和密文
                val iv = encryptedData.copyOfRange(0, 12)
                val cipherText = encryptedData.copyOfRange(12, encryptedData.size)
                
                val cipher = Cipher.getInstance(AES_MODE)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                
                val decryptedData = cipher.doFinal(cipherText)
                EncryptionResult.Success(decryptedData)
            } catch (e: Exception) {
                logger.e("EncryptionManager", "使用自定义密钥解密失败", e)
                EncryptionResult.Error("解密失败: ${e.message}")
            }
        }
    }
    
    /**
     * 计算数据的哈希值
     */
    fun calculateHash(data: ByteArray, algorithm: String = "SHA-256"): String {
        return try {
            val digest = java.security.MessageDigest.getInstance(algorithm)
            val hashBytes = digest.digest(data)
            hashBytes.toHexString()
        } catch (e: Exception) {
            logger.e("EncryptionManager", "计算哈希失败", e)
            throw e
        }
    }
    
    /**
     * 计算文件的哈希值
     */
    suspend fun calculateFileHash(file: File, algorithm: String = "SHA-256"): String {
        return withContext(Dispatchers.IO) {
            try {
                val digest = java.security.MessageDigest.getInstance(algorithm)
                FileInputStream(file).use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                
                val hashBytes = digest.digest()
                hashBytes.toHexString()
            } catch (e: Exception) {
                logger.e("EncryptionManager", "计算文件哈希失败", e)
                throw e
            }
        }
    }
    
    /**
     * 生成安全的随机密码
     */
    fun generateSecurePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSpecial: Boolean = true
    ): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val special = "!@#$%^&*()-_=+[]{}|;:,.<>?"
        
        var charPool = ""
        if (includeUppercase) charPool += uppercase
        if (includeLowercase) charPool += lowercase
        if (includeDigits) charPool += digits
        if (includeSpecial) charPool += special
        
        if (charPool.isEmpty()) {
            charPool = lowercase + digits
        }
        
        val random = java.security.SecureRandom()
        return (1..length)
            .map { charPool[random.nextInt(charPool.length)] }
            .joinToString("")
    }
    
    /**
     * 验证密码强度
     */
    fun validatePasswordStrength(password: String): PasswordStrength {
        var score = 0
        
        // 长度检查
        when {
            password.length >= 12 -> score += 2
            password.length >= 8 -> score += 1
            else -> return PasswordStrength.WEAK
        }
        
        // 字符种类检查
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigits = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        
        if (hasUppercase) score++
        if (hasLowercase) score++
        if (hasDigits) score++
        if (hasSpecial) score++
        
        // 常见模式检查
        val commonPatterns = listOf(
            "123456", "password", "qwerty", "admin", "welcome"
        )
        
        if (commonPatterns.any { password.contains(it, ignoreCase = true) }) {
            score -= 2
        }
        
        // 连续字符检查
        val hasSequential = (0 until password.length - 2).any { i ->
            val c1 = password[i].code
            val c2 = password[i + 1].code
            val c3 = password[i + 2].code
            (c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)
        }
        
        if (hasSequential) score--
        
        return when {
            score >= 6 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
    
    /**
     * 获取加密状态
     */
    fun getEncryptionStatus(): EncryptionStatus {
        return EncryptionStatus(
            isInitialized = isInitialized,
            hasMasterKey = hasKey(),
            algorithm = AES_MODE,
            keySize = KEY_SIZE
        )
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // Tink会自动管理资源
    }
}

// 扩展函数
private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

private fun String.hexToByteArray(): ByteArray {
    val len = length
    val data = ByteArray(len / 2)
    var i = 0
    
    while (i < len) {
        data[i / 2] = ((Character.digit(this[i], 16) shl 4) +
                Character.digit(this[i + 1], 16)).toByte()
        i += 2
    }
    
    return data
}

// 数据类
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}

data class EncryptionStatus(
    val isInitialized: Boolean,
    val hasMasterKey: Boolean,
    val algorithm: String,
    val keySize: Int
)