package clipto.backup

import com.wb.clipboard.R

enum class BackupItemType(val titleRes: Int) {

    SETTINGS(R.string.settings_toolbar_title),
    NOTES(R.string.main_filter_notes),
    TAGS(R.string.main_filter_tags),
    FILTERS(R.string.main_filter_filters),
    SNIPPET_KITS(R.string.clip_details_tab_snippet_kits)

}