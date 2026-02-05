package clipto.dynamic.models

import clipto.common.misc.FormatUtils
import clipto.common.misc.IdUtils
import com.wb.clipboard.R

enum class RandomType(val titleRes: Int, val type: String, val valueProvider: (values: List<String>) -> String) {

    DIGIT(R.string.dynamic_field_random_type_digit, "0-9", {
        val symbols = "0123456789"
        symbols[IdUtils.rand.nextInt(symbols.length)].toString()
    }),

    a_z_A_Z(R.string.dynamic_field_random_type_a_z_A_Z, "a-z, A-Z", {
        val symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        symbols[IdUtils.rand.nextInt(symbols.length)].toString()
    }),

    a_z(R.string.dynamic_field_random_type_a_z, "a-z", {
        val symbols = "abcdefghijklmnopqrstuvwxyz"
        symbols[IdUtils.rand.nextInt(symbols.length)].toString()
    }),

    A_Z(R.string.dynamic_field_random_type_A_Z, "A-Z", {
        val symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        symbols[IdUtils.rand.nextInt(symbols.length)].toString()
    }),

    CUSTOM(R.string.where_date_custom, "custom", {
        if (it.isEmpty()) {
            FormatUtils.UNKNOWN
        } else {
            it.random()
        }
    })
    ;

    companion object {

        fun byId(id: String?): RandomType? = values().find { it.type == id }

        fun byIdOrDefault(id: String?): RandomType = byId(id) ?: a_z_A_Z
    }

}