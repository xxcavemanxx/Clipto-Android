package clipto.extensions

import android.content.Context
import clipto.domain.ClipDetailsTab
import com.wb.clipboard.R

fun ClipDetailsTab.getLabel(context: Context): String {
    return when (this) {
        ClipDetailsTab.GENERAL -> context.getString(R.string.clip_details_tab_general)
        ClipDetailsTab.ATTACHMENTS -> context.getString(R.string.clip_details_tab_attachments)
        ClipDetailsTab.DYNAMIC_VALUES -> context.getString(R.string.clip_details_tab_dynamic)
        ClipDetailsTab.SNIPPETS -> context.getString(R.string.clip_details_tab_snippets)
        ClipDetailsTab.SNIPPET_KITS -> context.getString(R.string.clip_details_tab_snippet_kits)
        ClipDetailsTab.TAGS -> context.getString(R.string.clip_details_tab_tags)
        ClipDetailsTab.ATTRIBUTES -> context.getString(R.string.clip_details_tab_attributes)
        ClipDetailsTab.VALUES -> context.getString(R.string.clip_details_tab_dynamic_values)
        ClipDetailsTab.FIELDS -> context.getString(R.string.clip_details_tab_dynamic_fields)
        ClipDetailsTab.FOLDER -> context.getString(R.string.clip_details_tab_folder)
    }
}