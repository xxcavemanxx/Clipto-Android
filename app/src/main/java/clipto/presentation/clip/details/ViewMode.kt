package clipto.presentation.clip.details

import clipto.domain.ClipDetailsTab

enum class ViewMode(val position: Int) {

    GENERAL(0),
    ATTRIBUTES(1),
    ATTACHMENTS(2),
    DYNAMIC_VALUES(3);

    companion object {
        fun valueOf(tab: ClipDetailsTab) = when (tab) {
            ClipDetailsTab.GENERAL -> GENERAL
            ClipDetailsTab.DYNAMIC_VALUES -> DYNAMIC_VALUES
            ClipDetailsTab.SNIPPETS -> DYNAMIC_VALUES
            ClipDetailsTab.FIELDS -> DYNAMIC_VALUES
            ClipDetailsTab.VALUES -> DYNAMIC_VALUES
            ClipDetailsTab.ATTACHMENTS -> ATTACHMENTS
            ClipDetailsTab.ATTRIBUTES -> ATTRIBUTES
            ClipDetailsTab.SNIPPET_KITS -> ATTRIBUTES
            ClipDetailsTab.TAGS -> ATTRIBUTES
            ClipDetailsTab.FOLDER -> ATTRIBUTES
        }
    }

}