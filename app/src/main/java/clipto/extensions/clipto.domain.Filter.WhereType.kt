package clipto.extensions

import clipto.domain.Filter
import com.wb.clipboard.R

fun Filter.WhereType.getTitleRes(): Int {
    return when (this) {
        Filter.WhereType.ALL_OF -> R.string.where_all_of
        Filter.WhereType.ANY_OF -> R.string.where_any_of
        Filter.WhereType.NONE_OF -> R.string.where_none_of
    }
}