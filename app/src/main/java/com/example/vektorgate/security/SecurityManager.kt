package com.example.vektorgate.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class SecurityManager {

    companion object {
        private const val KEY_ALIAS = "VektorGateKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun generateKeyPair() {
        if (keyStore.containsAlias(KEY_ALIAS)) return

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).run {
            setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            setDigests(KeyProperties.DIGEST_SHA256)
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
            build()
        }

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }

    fun getPublicKeyBase64(): String? {
        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey ?: return null
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun getInitializedSignature(): Signature? {
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey ?: return null
        return Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(privateKey)
        }
    }

    /**
     * Finalizes the signature after the biometric unlock.
     */
    fun signData(signature: Signature, data: ByteArray): String {
        signature.update(data)
        val signatureBytes = signature.sign()
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }
    
    fun hasKey(): Boolean = keyStore.containsAlias(KEY_ALIAS)
}
