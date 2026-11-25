package com.ayyappasamaaj.tattvamasi.ui.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.ayyappasamaaj.tattvamasi.util.AppLog
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Where the app keeps the website's session token between runs: encrypted with a key that never leaves Android's
 * Keystore (hardware-backed on most phones), so a copy of the app's files is no use without the phone. The name and
 * email shown on My account are kept beside it (not secret). Cleared on signing out.
 */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("account", Context.MODE_PRIVATE)

    data class Saved(val token: String, val user: AccountUser)

    fun load(): Saved? {
        val sealed = prefs.getString(TOKEN, null) ?: return null
        val token = runCatching { open(sealed) }.onFailure { AppLog.w("TokenStore", "token: " + it.message) }.getOrNull()
        if (token == null) {  // the key is gone (e.g. the phone's lock screen was removed): sign in again
            clear()
            return null
        }
        return Saved(token, AccountUser(prefs.getString(NAME, "").orEmpty(), prefs.getString(EMAIL, "").orEmpty()))
    }

    fun save(token: String, user: AccountUser) {
        prefs.edit { putString(TOKEN, seal(token)).putString(NAME, user.name).putString(EMAIL, user.email) }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun seal(text: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun open(sealed: String): String {
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_BYTES))
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "account-token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TOKEN = "token"
        const val NAME = "name"
        const val EMAIL = "email"
    }
}
