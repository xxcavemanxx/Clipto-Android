package clipto.common.misc

import org.greenrobot.essentials.StringUtils
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object AesUtils {

    fun encrypt(password: String, text: String): String {
        val rawKey = SecretKeySpec(password.toByteArray(), "AES").encoded
        val result = encrypt(rawKey, text.toByteArray())
        return StringUtils.hex(result)
    }

    fun decrypt(password: String, text: String): String {
        val enc = toByte(text)
        val result = decrypt(password, enc)
        return String(result)
    }

    private fun encrypt(raw: ByteArray, clear: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        return cipher.doFinal(clear)
    }

    private fun decrypt(password: String, encrypted: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(password.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        return cipher.doFinal(encrypted)
    }

    private fun toByte(hexString: String): ByteArray {
        val len = hexString.length / 2
        val result = ByteArray(len)
        for (i in 0 until len) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).toByte()
        }
        return result
    }
}