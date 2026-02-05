package clipto.presentation.runes

import clipto.domain.IRune

data class RuneItem(
        val rune: IRune,
        val isActive: Boolean,
        val hasWarning:Boolean
)
