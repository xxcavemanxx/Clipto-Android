package clipto.common.misc

import org.greenrobot.essentials.StringUtils
import java.security.SecureRandom

object IdUtils {

    val rand = SecureRandom()

    fun autoId(string: String? = null): String {
        if (string != null) return id(string)
        val builder = StringBuilder()
        val maxRandom = AUTO_ID_ALPHABET.length
        for (i in 0..19) {
            builder.append(AUTO_ID_ALPHABET[rand.nextInt(maxRandom)])
        }
        return builder.toString()
    }

    fun id(string: String): String = StringUtils.sha1(string).toUpperCase()

    private const val AUTO_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
}