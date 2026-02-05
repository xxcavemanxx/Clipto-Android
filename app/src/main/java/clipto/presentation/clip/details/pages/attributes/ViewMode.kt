package clipto.presentation.clip.details.pages.attributes

import clipto.domain.ClipDetailsTab

enum class ViewMode(val position: Int, val tab: ClipDetailsTab) {

    TAGS(0, ClipDetailsTab.TAGS),

    FOLDER(1, ClipDetailsTab.FOLDER),

    KITS(2, ClipDetailsTab.SNIPPET_KITS);

    companion object {
        fun valueOf(tab: ClipDetailsTab): ViewMode = values().firstOrNull { it.tab == tab } ?: TAGS
    }

}