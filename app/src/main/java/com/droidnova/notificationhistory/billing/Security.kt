package com.droidnova.notificationhistory.billing

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object Security {
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (base64PublicKey.isBlank() || signedData.isBlank() || signature.isBlank()) {
            return false
        }
        return try {
            val publicKey = generatePublicKey(base64PublicKey)
            val signatureBytes = Base64.decode(signature, Base64.DEFAULT)
            val signatureAlgorithm = Signature.getInstance("SHA1withRSA")
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            signatureAlgorithm.verify(signatureBytes)
        } catch (_: Exception) {
            false
        }
    }

    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decodedKey)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }
}
