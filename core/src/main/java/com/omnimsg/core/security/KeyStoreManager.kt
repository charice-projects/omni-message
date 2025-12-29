// 📁 core/security/KeyStoreManager.kt
package com.omnimsg.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.*
import java.security.cert.Certificate
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyStoreManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SHARED_PREFS_NAME = "encrypted_prefs"
        
        // 密钥别名
        const val KEY_ALIAS_DATABASE = "database_key"
        const val KEY_ALIAS_BIOMETRIC = "biometric_key"
        const val KEY_ALIAS_ENCRYPTION = "encryption_key"
        const val KEY_ALIAS_SIGNATURE = "signature_key"
        const val KEY_ALIAS_BACKUP = "backup_key"
    }
    
    sealed class KeyStoreResult {
        data class Success(val key: Key) : KeyStoreResult()
        data class Error(val message: String) : KeyStoreResult()
    }
    
    data class KeyInfo(
        val alias: String,
        val algorithm: String,
        val keySize: Int,
        val purposes: List<KeyPurpose>,
        val isHardwareBacked: Boolean,
        val creationDate: Long
    )
    
    enum class KeyPurpose {
        ENCRYPT, DECRYPT, SIGN, VERIFY, AUTHENTICATE
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    private var encryptedPrefs: SharedPreferences? = null
    
    /**
     * 初始化密钥存储管理器
     */
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 初始化加密的SharedPreferences
                encryptedPrefs = createEncryptedPreferences()
                
                // 生成必要的密钥
                generateEssentialKeys()
                
                logger.i("KeyStoreManager", "密钥存储管理器初始化成功")
                true
            } catch (e: Exception) {
                logger.e("KeyStoreManager", "密钥存储管理器初始化失败", e)
                false
            }
        }
    }
    
    /**
     * 创建加密的SharedPreferences
     */
    private fun createEncryptedPreferences(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        return EncryptedSharedPreferences.create(
            SHARED_PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * 生成必要的密钥
     */
    private fun generateEssentialKeys() {
        // 生成数据库加密密钥
        if (!hasKey(KEY_ALIAS_DATABASE)) {
            generateDatabaseKey()
        }
        
        // 生成生物识别密钥
        if (!hasKey(KEY_ALIAS_BIOMETRIC)) {
            generateBiometricKey()
        }
        
        // 生成加密密钥
        if (!hasKey(KEY_ALIAS_ENCRYPTION)) {
            generateEncryptionKey()
        }
    }
    
    /**
     * 生成数据库加密密钥
     */
    fun generateDatabaseKey(): Boolean {
        return generateSymmetricKey(
            alias = KEY_ALIAS_DATABASE,
            purposes = listOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
            keySize = 256,
            requireUserAuthentication = false
        )
    }
    
    /**
     * 生成生物识别密钥
     */
    fun generateBiometricKey(): Boolean {
        return generateSymmetricKey(
            alias = KEY_ALIAS_BIOMETRIC,
            purposes = listOf(KeyPurpose.AUTHENTICATE),
            keySize = 256,
            requireUserAuthentication = true,
            userAuthenticationValiditySeconds = 30,
            invalidatedByBiometricEnrollment = true
        )
    }
    
    /**
     * 生成加密密钥
     */
    fun generateEncryptionKey(): Boolean {
        return generateSymmetricKey(
            alias = KEY_ALIAS_ENCRYPTION,
            purposes = listOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT),
            keySize = 256,
            requireUserAuthentication = false
        )
    }
    
    /**
     * 生成签名密钥对
     */
    fun generateSignatureKeyPair(alias: String = KEY_ALIAS_SIGNATURE): Boolean {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .setUserAuthenticationRequired(false)
                .build()
            
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
            
            true
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "生成签名密钥对失败", e)
            false
        }
    }
    
    /**
     * 生成对称密钥
     */
    fun generateSymmetricKey(
        alias: String,
        purposes: List<KeyPurpose>,
        keySize: Int = 256,
        requireUserAuthentication: Boolean = false,
        userAuthenticationValiditySeconds: Int = 0,
        invalidatedByBiometricEnrollment: Boolean = true
    ): Boolean {
        return try {
            val keyPurposes = purposes.fold(0) { acc, purpose ->
                acc or when (purpose) {
                    KeyPurpose.ENCRYPT -> KeyProperties.PURPOSE_ENCRYPT
                    KeyPurpose.DECRYPT -> KeyProperties.PURPOSE_DECRYPT
                    KeyPurpose.SIGN -> KeyProperties.PURPOSE_SIGN
                    KeyPurpose.VERIFY -> KeyProperties.PURPOSE_VERIFY
                    KeyPurpose.AUTHENTICATE -> KeyProperties.PURPOSE_ENCRYPT
                }
            }
            
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val builder = KeyGenParameterSpec.Builder(alias, keyPurposes)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(keySize)
                .setUserAuthenticationRequired(requireUserAuthentication)
            
            if (requireUserAuthentication && userAuthenticationValiditySeconds > 0) {
                builder.setUserAuthenticationValidityDurationSeconds(
                    userAuthenticationValiditySeconds
                )
            }
            
            if (requireUserAuthentication) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
            }
            
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            
            true
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "生成对称密钥失败", e)
            false
        }
    }
    
    /**
     * 获取密钥
     */
    fun getKey(alias: String): KeyStoreResult {
        return try {
            if (!keyStore.containsAlias(alias)) {
                return KeyStoreResult.Error("密钥不存在: $alias")
            }
            
            val entry = keyStore.getEntry(alias, null)
            val key = when (entry) {
                is KeyStore.SecretKeyEntry -> entry.secretKey
                is KeyStore.PrivateKeyEntry -> entry.privateKey
                is KeyStore.TrustedCertificateEntry -> entry.certificate
                else -> throw IllegalStateException("未知的密钥条目类型")
            }
            
            KeyStoreResult.Success(key)
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "获取密钥失败", e)
            KeyStoreResult.Error("获取密钥失败: ${e.message}")
        }
    }
    
    /**
     * 获取数据库加密密钥
     */
    fun getDatabaseKey(): ByteArray {
        return try {
            val result = getKey(KEY_ALIAS_DATABASE)
            when (result) {
                is KeyStoreResult.Success -> {
                    val key = result.key as? SecretKey
                    key?.encoded ?: throw IllegalStateException("无效的数据库密钥")
                }
                is KeyStoreResult.Error -> throw IllegalStateException(result.message)
            }
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "获取数据库密钥失败", e)
            // 返回默认密钥（仅用于开发环境）
            if (BuildConfig.DEBUG) {
                "default_database_key_for_debug".toByteArray()
            } else {
                throw e
            }
        }
    }
    
    /**
     * 检查密钥是否存在
     */
    fun hasKey(alias: String): Boolean {
        return try {
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "检查密钥失败", e)
            false
        }
    }
    
    /**
     * 删除密钥
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "删除密钥失败", e)
            false
        }
    }
    
    /**
     * 获取密钥信息
     */
    fun getKeyInfo(alias: String): KeyInfo? {
        return try {
            if (!keyStore.containsAlias(alias)) {
                return null
            }
            
            val entry = keyStore.getEntry(alias, null)
            val key = when (entry) {
                is KeyStore.SecretKeyEntry -> entry.secretKey
                is KeyStore.PrivateKeyEntry -> entry.privateKey
                else -> return null
            }
            
            val certChain = if (entry is KeyStore.PrivateKeyEntry) {
                entry.certificateChain
            } else {
                null
            }
            
            KeyInfo(
                alias = alias,
                algorithm = key.algorithm,
                keySize = getKeySize(key),
                purposes = getKeyPurposes(alias),
                isHardwareBacked = isHardwareBacked(key),
                creationDate = certChain?.firstOrNull()?.notBefore?.time ?: 0
            )
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "获取密钥信息失败", e)
            null
        }
    }
    
    /**
     * 获取密钥大小
     */
    private fun getKeySize(key: Key): Int {
        return when (key) {
            is SecretKey -> key.encoded?.size?.times(8) ?: 256
            is PrivateKey -> {
                when (key.algorithm) {
                    "RSA" -> (key as? java.security.interfaces.RSAKey)?.modulus?.bitLength() ?: 2048
                    "EC" -> (key as? java.security.interfaces.ECKey)?.params?.curve?.field?.fieldSize ?: 256
                    else -> 0
                }
            }
            else -> 0
        }
    }
    
    /**
     * 获取密钥用途
     */
    private fun getKeyPurposes(alias: String): List<KeyPurpose> {
        // 这里应该从KeyStore获取密钥的实际用途
        // 简化实现：根据别名判断
        return when (alias) {
            KEY_ALIAS_DATABASE -> listOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT)
            KEY_ALIAS_BIOMETRIC -> listOf(KeyPurpose.AUTHENTICATE)
            KEY_ALIAS_ENCRYPTION -> listOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT)
            KEY_ALIAS_SIGNATURE -> listOf(KeyPurpose.SIGN, KeyPurpose.VERIFY)
            else -> emptyList()
        }
    }
    
    /**
     * 检查是否为硬件支持的密钥
     */
    private fun isHardwareBacked(key: Key): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                key is PrivateKey && KeyProperties.KEY_ALGORITHM_RSA == key.algorithm
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 存储安全数据到加密的SharedPreferences
     */
    fun storeSecureData(key: String, value: String): Boolean {
        return try {
            encryptedPrefs?.edit()?.putString(key, value)?.apply()
            true
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "存储安全数据失败", e)
            false
        }
    }
    
    /**
     * 从加密的SharedPreferences获取安全数据
     */
    fun getSecureData(key: String, defaultValue: String = ""): String {
        return try {
            encryptedPrefs?.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "获取安全数据失败", e)
            defaultValue
        }
    }
    
    /**
     * 删除安全数据
     */
    fun deleteSecureData(key: String): Boolean {
        return try {
            encryptedPrefs?.edit()?.remove(key)?.apply()
            true
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "删除安全数据失败", e)
            false
        }
    }
    
    /**
     * 生成安全的随机密钥
     */
    fun generateSecureRandomKey(size: Int = 32): ByteArray {
        return ByteArray(size).apply {
            SecureRandom().nextBytes(this)
        }
    }
    
    /**
     * 导出公钥证书
     */
    fun exportPublicKeyCertificate(alias: String): Certificate? {
        return try {
            if (!keyStore.containsAlias(alias)) {
                return null
            }
            
            val entry = keyStore.getEntry(alias, null)
            if (entry is KeyStore.PrivateKeyEntry) {
                entry.certificate
            } else {
                null
            }
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "导出公钥证书失败", e)
            null
        }
    }
    
    /**
     * 签名数据
     */
    fun signData(alias: String, data: ByteArray): ByteArray? {
        return try {
            val result = getKey(alias)
            when (result) {
                is KeyStoreResult.Success -> {
                    val privateKey = result.key as? PrivateKey
                    if (privateKey != null) {
                        val signature = Signature.getInstance("SHA256withRSA")
                        signature.initSign(privateKey)
                        signature.update(data)
                        signature.sign()
                    } else {
                        null
                    }
                }
                is KeyStoreResult.Error -> null
            }
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "签名数据失败", e)
            null
        }
    }
    
    /**
     * 验证签名
     */
    fun verifySignature(alias: String, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            if (!keyStore.containsAlias(alias)) {
                return false
            }
            
            val cert = keyStore.getCertificate(alias)
            if (cert != null) {
                val verifier = Signature.getInstance("SHA256withRSA")
                verifier.initVerify(cert.publicKey)
                verifier.update(data)
                verifier.verify(signature)
            } else {
                false
            }
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "验证签名失败", e)
            false
        }
    }
    
    /**
     * 获取所有密钥别名
     */
    fun getAllKeyAliases(): List<String> {
        return try {
            keyStore.aliases().toList()
        } catch (e: Exception) {
            logger.e("KeyStoreManager", "获取密钥别名失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取密钥存储状态
     */
    fun getKeyStoreStatus(): KeyStoreStatus {
        val aliases = getAllKeyAliases()
        val essentialKeys = listOf(
            KEY_ALIAS_DATABASE,
            KEY_ALIAS_BIOMETRIC,
            KEY_ALIAS_ENCRYPTION,
            KEY_ALIAS_SIGNATURE
        )
        
        val missingKeys = essentialKeys.filter { !aliases.contains(it) }
        
        return KeyStoreStatus(
            isInitialized = encryptedPrefs != null,
            totalKeys = aliases.size,
            essentialKeysConfigured = missingKeys.isEmpty(),
            missingKeys = missingKeys,
            hardwareBacked = aliases.any { alias ->
                val key = getKey(alias)
                when (key) {
                    is KeyStoreResult.Success -> isHardwareBacked(key.key)
                    else -> false
                }
            }
        )
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // KeyStore会自动管理资源
    }
}

// 数据类
data class KeyStoreStatus(
    val isInitialized: Boolean,
    val totalKeys: Int,
    val essentialKeysConfigured: Boolean,
    val missingKeys: List<String>,
    val hardwareBacked: Boolean
)