package clipto.presentation.runes

import clipto.domain.IRune

data class RuneFlatItem(
        val rune: IRune,
        val isActive: Boolean,
        val hasWarning: Boolean,
        val expanded: Boolean
)
