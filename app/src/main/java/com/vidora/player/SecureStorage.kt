package com.vidora.player

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureStorage {

    private const val PREFS =
        "vidora_secure_storage"

    private const val KEY_NAME =
        "VidoraVaultKey"

    private const val VALUE =
        "vault_password"

    private const val TRANSFORMATION =
        "AES/GCM/NoPadding"

    private const val IV_SIZE =
        12

    private fun preferences(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    private fun getOrCreateKey(): SecretKey {

        val keyStore =
            java.security.KeyStore
                .getInstance(
                    "AndroidKeyStore"
                )
                .apply {
                    load(null)
                }

        val existing =
            keyStore.getKey(
                KEY_NAME,
                null
            )

        if (
            existing is SecretKey
        ) {
            return existing
        }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

        val spec =
            KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setUserAuthenticationRequired(
                    false
                )
                .build()

        generator.init(
            spec
        )

        return generator.generateKey()
    }

    fun savePassword(
        context: Context,
        password: String
    ) {

        if (
            password.isEmpty()
        ) {
            clearPassword(
                context
            )
            return
        }

        val key =
            getOrCreateKey()

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            key
        )

        val encrypted =
            cipher.doFinal(
                password.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        val iv =
            cipher.iv

        val combined =
            ByteArray(
                iv.size +
                    encrypted.size
            )

        System.arraycopy(
            iv,
            0,
            combined,
            0,
            iv.size
        )

        System.arraycopy(
            encrypted,
            0,
            combined,
            iv.size,
            encrypted.size
        )

        val encoded =
            Base64.encodeToString(
                combined,
                Base64.NO_WRAP
            )

        preferences(context)
            .edit()
            .putString(
                VALUE,
                encoded
            )
            .apply()
    }

    fun verifyPassword(
        context: Context,
        password: String
    ): Boolean {

        val stored =
            preferences(context)
                .getString(
                    VALUE,
                    null
                )
                ?: return false

        return try {

            val combined =
                Base64.decode(
                    stored,
                    Base64.NO_WRAP
                )

            if (
                combined.size <=
                IV_SIZE
            ) {
                return false
            }

            val iv =
                combined.copyOfRange(
                    0,
                    IV_SIZE
                )

            val encrypted =
                combined.copyOfRange(
                    IV_SIZE,
                    combined.size
                )

            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    128,
                    iv
                )
            )

            val decrypted =
                cipher.doFinal(
                    encrypted
                )

            String(
                decrypted,
                StandardCharsets.UTF_8
            ) == password

        } catch (
            _: Exception
        ) {
            false
        }
    }

    fun hasPassword(
        context: Context
    ): Boolean {

        return !preferences(context)
            .getString(
                VALUE,
                null
            )
            .isNullOrEmpty()
    }

    fun clearPassword(
        context: Context
    ) {

        preferences(context)
            .edit()
            .remove(
                VALUE
            )
            .apply()
    }
}
