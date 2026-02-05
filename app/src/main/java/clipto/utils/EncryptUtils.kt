package clipto.utils

import android.util.Base64
import com.wb.clipboard.BuildConfig
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

object EncryptUtils {

    fun encryptText(text: String): String {
        return text
    }

    fun decryptText(text: String): String {
        return text
    }

    fun encryptDes(text: String): String {
        val keySpec = DESKeySpec(BuildConfig.APPLICATION_ID.toByteArray(charset("UTF8")))
        val keyFactory = SecretKeyFactory.getInstance("DES")
        val key = keyFactory.generateSecret(keySpec)
        val cleartext = text.toByteArray(charset("UTF8"))
        val cipher = Cipher.getInstance("DES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return Base64.encodeToString(cipher.doFinal(cleartext), Base64.DEFAULT).trim()
    }

}