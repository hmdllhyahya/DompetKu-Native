package com.dompetku.util

import java.security.MessageDigest

/**
 * PIN hashing — format MUST NOT change:
 *   SHA-256( salt + ":" + pin )
 * where salt = "dompetku_salt_2024"
 *
 * This matches the JSX implementation exactly so that PINs set in the web
 * version remain valid after migration.
 */
object PinHasher {
    private const val SALT = "dompetku_salt_2024"

    fun hash(pin: String): String {
        val input = "$SALT:$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, storedHash: String): Boolean = hash(pin) == storedHash
}
