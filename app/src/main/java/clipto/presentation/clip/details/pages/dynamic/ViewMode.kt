package clipto.presentation.clip.details.pages.dynamic

import clipto.domain.ClipDetailsTab

enum class ViewMode(val position: Int, val tab: ClipDetailsTab) {

    VALUES(0, ClipDetailsTab.VALUES),

    FIELDS(1, ClipDetailsTab.FIELDS),

    SNIPPETS(2, ClipDetailsTab.SNIPPETS);

    companion object {
        fun valueOf(tab: ClipDetailsTab): ViewMode = values().firstOrNull { it.tab == tab } ?: VALUES
    }

}