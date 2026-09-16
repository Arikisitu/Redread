package io.github.landwarderer.futon.settings.sources.access

import android.util.Base64
import io.github.landwarderer.futon.core.prefs.AppSettings
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceAccessManager @Inject constructor(
	private val settings: AppSettings,
) {

	var isUnlocked = false
		private set

	val needsPasswordSetup: Boolean
		get() = settings.sourceAccessPasswordHash.isNullOrEmpty() || settings.sourceAccessPasswordSalt.isNullOrEmpty()

	fun setPassword(password: CharArray) {
		val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
		settings.sourceAccessPasswordSalt = Base64.encodeToString(salt, Base64.NO_WRAP)
		settings.sourceAccessPasswordHash = Base64.encodeToString(hash(password, salt), Base64.NO_WRAP)
		password.fill('\u0000')
		isUnlocked = true
	}

	fun unlock(password: CharArray): Boolean {
		val encodedSalt = settings.sourceAccessPasswordSalt ?: return false
		val encodedHash = settings.sourceAccessPasswordHash ?: return false
		val salt = Base64.decode(encodedSalt, Base64.NO_WRAP)
		val expected = Base64.decode(encodedHash, Base64.NO_WRAP)
		val result = MessageDigest.isEqual(hash(password, salt), expected)
		password.fill('\u0000')
		isUnlocked = result
		return result
	}

	private fun hash(password: CharArray, salt: ByteArray): ByteArray = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
		.let { spec ->
			try {
				SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
			} finally {
				spec.clearPassword()
			}
		}

	private companion object {
		const val SALT_BYTES = 16
		const val ITERATIONS = 210_000
		const val KEY_LENGTH_BITS = 256
	}
}
