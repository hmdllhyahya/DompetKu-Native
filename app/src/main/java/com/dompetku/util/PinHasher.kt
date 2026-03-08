package com.dompetku.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN hashing — PBKDF2WithHmacSHA256, 100.000 iterasi, 16-byte random salt per hash.
 *
 * Format hash baru: "pbkdf2$<iterations>$<salt_hex>$<hash_hex>"
 * Contoh: "pbkdf2$100000$a3f1c2d4e5b6a7c8...$9f8e7d6c5b4a3..."
 *
 * ── Migration dari SHA-256 lama ──────────────────────────────────────────────
 * Hash lama (SHA-256 + static salt) tidak punya prefix "pbkdf2$".
 * [verify] otomatis mendeteksi format dan menggunakan verifikasi yang sesuai.
 * [verifyAndUpgrade] juga re-hash ke PBKDF2 jika PIN benar dan hash masih lama.
 *
 * Gunakan [verifyAndUpgrade] di PinViewModel agar semua user lama ter-upgrade
 * secara transparan saat login berikutnya.
 */
object PinHasher {

    // ── PBKDF2 params ─────────────────────────────────────────────────────────
    private const val ALGORITHM   = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS  = 100_000
    private const val KEY_LENGTH  = 256          // bits → 32 bytes
    private const val SALT_BYTES  = 16
    private const val PREFIX      = "pbkdf2"

    // ── Legacy SHA-256 params (read-only, untuk migration) ────────────────────
    private const val LEGACY_SALT = "dompetku_salt_2024"

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Hash PIN baru dengan PBKDF2 + random salt.
     * Selalu gunakan ini saat menyimpan PIN baru atau mengganti PIN.
     */
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        return "$PREFIX$$ITERATIONS$${salt.toHex()}$${hash.toHex()}"
    }

    /**
     * Verifikasi PIN terhadap hash tersimpan.
     * Mendukung hash PBKDF2 baru MAUPUN hash SHA-256 lama.
     */
    fun verify(pin: String, storedHash: String): Boolean {
        return if (storedHash.startsWith("$PREFIX$")) {
            verifyPbkdf2(pin, storedHash)
        } else {
            verifyLegacy(pin, storedHash)
        }
    }

    /**
     * Verifikasi PIN, dan jika hash masih format lama (SHA-256), panggil [onUpgrade]
     * dengan hash PBKDF2 yang baru agar bisa disimpan ke DataStore.
     *
     * Contoh di PinViewModel:
     * ```
     * PinHasher.verifyAndUpgrade(entered, storedHash) { newHash ->
     *     prefs.setPinHash(newHash)
     * }
     * ```
     */
    suspend fun verifyAndUpgrade(
        pin:        String,
        storedHash: String,
        onUpgrade:  suspend (newHash: String) -> Unit,
    ): Boolean {
        return if (storedHash.startsWith("$PREFIX$")) {
            // Sudah PBKDF2 — cukup verifikasi biasa
            verifyPbkdf2(pin, storedHash)
        } else {
            // Hash lama — verifikasi dengan SHA-256
            val ok = verifyLegacy(pin, storedHash)
            if (ok) {
                // PIN benar → upgrade ke PBKDF2 secara transparan
                onUpgrade(hash(pin))
            }
            ok
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun verifyPbkdf2(pin: String, storedHash: String): Boolean {
        // Format: "pbkdf2$<iter>$<salt_hex>$<hash_hex>"
        val parts = storedHash.split("$")
        if (parts.size != 4) return false
        return runCatching {
            val iterations = parts[1].toInt()
            val salt       = parts[2].fromHex()
            val expected   = parts[3].fromHex()
            val actual     = pbkdf2(pin, salt, iterations)
            // Constant-time comparison to prevent timing attacks
            constantTimeEquals(actual, expected)
        }.getOrDefault(false)
    }

    private fun verifyLegacy(pin: String, storedHash: String): Boolean {
        val input  = "$LEGACY_SALT:$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes  = digest.digest(input.toByteArray(Charsets.UTF_8))
        val hashed = bytes.toHex()
        return constantTimeEquals(hashed.toByteArray(), storedHash.toByteArray())
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec    = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val result  = factory.generateSecret(spec).encoded
        spec.clearPassword()   // zero out pin from memory
        return result
    }

    /** Constant-time byte array comparison — prevents timing side-channel. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun constantTimeEquals(a: String, b: String) =
        constantTimeEquals(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0) { "Hex string panjang ganjil" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
