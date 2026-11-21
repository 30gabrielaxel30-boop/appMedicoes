package com.keyhistory.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Criptografia do conteúdo armazenado.
 *
 * Modelo de chaves (envelope encryption):
 *  - KEK: chave AES-256-GCM gerada e mantida dentro do Android Keystore (não exportável).
 *  - DEK: chave AES-256 aleatória usada para cifrar cada registro com AES-GCM (IV de 96 bits
 *    aleatório por registro, tag de 128 bits). A DEK fica armazenada em disco apenas
 *    cifrada pela KEK.
 *  - Chave de índice: chave HMAC-SHA256 aleatória (também cifrada pela KEK) usada para gerar
 *    tokens de pesquisa cegos, permitindo pesquisar no banco sem guardar texto em claro.
 */
class CryptoManager(context: Context) {

    private val keyFile = File(context.noBackupFilesDir, KEY_FILE)
    private val random = SecureRandom()

    private val keys: Pair<SecretKey, SecretKey> by lazy { loadOrCreateKeys() }
    private val dataKey: SecretKey get() = keys.first
    private val indexKey: SecretKey get() = keys.second

    fun encrypt(plainText: String): String {
        val iv = ByteArray(IV_SIZE).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, dataKey, GCMParameterSpec(TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(payload: String): String {
        require(payload.startsWith(PREFIX)) { "Formato de conteúdo cifrado desconhecido" }
        val bytes = Base64.decode(payload.removePrefix(PREFIX), Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipherText = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, dataKey, GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    /** Token de pesquisa cego (HMAC-SHA256 truncado, Base64). */
    fun blindToken(term: String): String {
        val mac = Mac.getInstance(HMAC)
        mac.init(indexKey)
        val digest = mac.doFinal(term.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest.copyOf(16), Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /** Verificação de integridade: cifra e decifra um valor de teste. */
    fun selfTest(): Boolean = runCatching {
        val sample = "KeyHistory-" + random.nextInt()
        decrypt(encrypt(sample)) == sample
    }.getOrDefault(false)

    // ---------------------------------------------------------------- chaves

    @Synchronized
    private fun loadOrCreateKeys(): Pair<SecretKey, SecretKey> {
        val kek = getOrCreateKek()
        if (keyFile.exists()) {
            runCatching {
                val raw = unwrap(kek, keyFile.readText())
                if (raw.size == 64) {
                    return SecretKeySpec(raw, 0, 32, "AES") to SecretKeySpec(raw, 32, 32, HMAC)
                }
            }
        }
        val raw = ByteArray(64).also { random.nextBytes(it) }
        keyFile.writeText(wrap(kek, raw))
        return SecretKeySpec(raw, 0, 32, "AES") to SecretKeySpec(raw, 32, 32, HMAC)
    }

    private fun getOrCreateKek(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEK_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEK_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun wrap(kek: SecretKey, raw: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        val out = cipher.iv + cipher.doFinal(raw)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun unwrap(kek: SecretKey, encoded: String): ByteArray {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_SIZE)))
        return cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size))
    }

    companion object {
        const val PREFIX = "v1:"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEK_ALIAS = "keyhistory_kek_v1"
        private const val KEY_FILE = "keyhistory_keys.bin"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val HMAC = "HmacSHA256"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
