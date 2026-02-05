package clipto.presentation.clip.details.pages.general.blocks

import android.view.View
import clipto.domain.Clip
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.clip.details.pages.general.GeneralPageFragment
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueString
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_general_attrs.view.*
import java.util.*

class AttrsBlock(
        private val clip: Clip,
        private val usageCount: Int = clip.usageCount,
        private val deleteDate: Date? = clip.deleteDate,
        private val updateDate: Date? = clip.modifyDate,
        private val textLength: Int = clip.text?.length ?: 0,
        private val deleted: Boolean = clip.isDeleted()
) : BlockItem<GeneralPageFragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_general_attrs

    override fun areContentsTheSame(item: BlockItem<GeneralPageFragment>): Boolean =
            item is AttrsBlock &&
                    item.deleted == deleted &&
                    item.updateDate == updateDate &&
                    item.usageCount == usageCount &&
                    item.textLength == textLength &&
                    item.deleteDate == deleteDate

    override fun onBind(fragment: GeneralPageFragment, block: View) {
        val ctx = block.context

        if (deleted) {
            KeyValueString(
                    block.updateDateTextView,
                    "\n",
                    ctx.getTextColorPrimary(),
                    ctx.getTextColorSecondary()).apply {
                setKey(StyleHelper.getDateValue(deleteDate))
                setValue(ctx.getString(R.string.clip_attr_deleted))
            }
        } else {
            KeyValueString(
                    block.updateDateTextView,
                    "\n",
                    ctx.getTextColorPrimary(),
                    ctx.getTextColorSecondary()).apply {
                setKey(StyleHelper.getDateValue(updateDate))
                setValue(ctx.getString(R.string.clip_attr_edited))
            }
        }

        KeyValueString(
                block.usageCountTextView,
                "\n",
                ctx.getTextColorPrimary(),
                ctx.getTextColorSecondary()).apply {
            setKey("$usageCount")
            setValue(ctx.getString(R.string.clip_attr_usageCount))
        }

        KeyValueString(
                block.charsCountTextView,
                "\n",
                ctx.getTextColorPrimary(),
                ctx.getTextColorSecondary()).apply {
            setKey("$textLength")
            setValue(ctx.getString(R.string.clip_attr_charsCount))
        }
    }

}