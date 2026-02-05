package clipto.common.extensions

fun CharSequence?.toNullIfEmpty(trim: Boolean = true): String? = this
        ?.run {
            when {
                isEmpty() -> null
                trim -> trim().toString().takeIf { it.isNotEmpty() }
                else -> toString()
            }
        }

fun CharSequence?.hasLocalFileReference(): Boolean = this?.run { contains("file:") || contains("content:") } == true