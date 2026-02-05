package clipto.presentation.runes.keyboard_companion.data

import clipto.domain.Filter

data class FilterData(
        var isActive: Boolean,
        val filter: Filter,
        val iconColor: Int,
        val iconRes: Int,
        val count: Long
)
