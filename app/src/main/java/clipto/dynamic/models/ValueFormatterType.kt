package clipto.dynamic.models

import com.wb.clipboard.R

enum class ValueFormatterType(
        val titleRes: Int,
        val separator: String,
        val formatter: (options: List<String>) -> String
) {

    NEW_LINE(R.string.dynamic_field_value_formatter_new_line, "new_line", formatter = { it.joinToString("\n") }),

    SEMICOLON(R.string.dynamic_field_value_formatter_semicolon, "semicolon", formatter = { it.joinToString("; ") }),

    COMMA(R.string.dynamic_field_value_formatter_comma, "comma", formatter = { it.joinToString(", ") }),

    SPACE(R.string.dynamic_field_value_formatter_space, "space", formatter = { it.joinToString(" ") }),

    ORDERED_LIST(R.string.dynamic_field_value_formatter_ordered_list, "ordered_list", formatter = { options ->
        val sb = StringBuilder()
        options.forEachIndexed { index, s ->
            sb.append(index + 1)
            sb.append(". ")
            sb.append(s)
            if (index < options.size - 1) {
                sb.appendLine()
            }
        }
        sb.toString()
    }),

    UNORDERED_LIST(R.string.dynamic_field_value_formatter_unordered_list, "unordered_list", formatter = { options ->
        val sb = StringBuilder()
        options.forEachIndexed { index, s ->
            sb.append("- ")
            sb.append(s)
            if (index < options.size - 1) {
                sb.appendLine()
            }
        }
        sb.toString()
    }),

    ;

    companion object {
        fun getByPlaceholder(placeholder: String): ValueFormatterType? = values().find { it.separator == placeholder }
    }

}