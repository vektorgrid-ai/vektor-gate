package com.example.vektorgate.security

import com.example.vektorgate.security.biometric.BiometricPromptManager
import kotlinx.serialization.InternalSerializationApi
import java.time.LocalDateTime

class ApprovalHandler(
    private val securityManager: SecurityManager,
    private val biometricPromptManager: BiometricPromptManager
) {

    /**
     * Validates the request based on basic security rules.
     */
    @OptIn(InternalSerializationApi::class)
    fun validateRequest(request: ApprovalRequest): Pair<Boolean, String?> {
        if (request.expiresAt < LocalDateTime.now()) {
            return false to "Request has expired."
        }
        
        if (!securityManager.hasKey()) {
            return false to "Security key not found. Please register first."
        }
        
        // TODO: validate request hash
        
        return true to null
    }

    /**
     * Starts the biometric signing process.
     */
    @OptIn(InternalSerializationApi::class)
    fun approveRequest(
        request: ApprovalRequest,
        onError: (String) -> Unit
    ) {
        val (isValid, error) = validateRequest(request)
        if (!isValid) {
            onError(error ?: "Invalid request")
            return
        }

        val signature = securityManager.getInitializedSignature()
        if (signature == null) {
            onError("Could not initialize security hardware.")
            return
        }

        // We show the human exactly what they are approving
        biometricPromptManager.showBiometricPrompt(
            title = "Confirm Approval",
            description = "Action: ${request.tool.description}\nRisk: ${request.tool.riskLevel.uppercase()}",
            cryptoObject = androidx.biometric.BiometricPrompt.CryptoObject(signature)
        )
    }

    /**
     * Finalizes the response. We sign a combination of the nonce and request_id 
     * to ensure the signature is unique to this specific request.
     */
    @OptIn(InternalSerializationApi::class)
    fun processResult(
        result: BiometricPromptManager.BiometricResult,
        request: ApprovalRequest,
        deviceId: String
    ): ApprovalResponse? {
        if (result is BiometricPromptManager.BiometricResult.AuthenticationSuccess) {
            val unlockedSignature = result.cryptoObject?.signature ?: return null

            val dataToSign = "${request.requestId}|${request.nonce}|${request.payloadHash}"
            
            val signatureBase64 = securityManager.signData(
                unlockedSignature, 
                dataToSign.toByteArray()
            )

            return ApprovalResponse(
                request_id = request.requestId,
                decision = "approve",
                timestamp = System.currentTimeMillis() / 1000,
                signature = signatureBase64,
                public_key = securityManager.getPublicKeyBase64() ?: "",
                device_id = deviceId
            )
        }
        return null
    }
}
