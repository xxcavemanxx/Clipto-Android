package clipto.presentation.clip.details.pages.general.blocks

import android.content.Context
import android.view.View
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.domain.FastAction
import clipto.domain.Clip
import clipto.domain.TextType
import clipto.presentation.clip.details.pages.general.GeneralPageFragment
import clipto.presentation.clip.details.pages.general.GeneralPageViewModel
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_general_export.view.*

class ExportBlock(
        private val viewModel: GeneralPageViewModel,
        private val clip: Clip,
        private val type: TextType = clip.textType
) : BlockItem<GeneralPageFragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_general_export

    override fun areContentsTheSame(item: BlockItem<GeneralPageFragment>): Boolean =
            item is ExportBlock &&
                    item.type == type

    override fun onBind(fragment: GeneralPageFragment, block: View) {
        val action1 = block.action1
        val action2 = block.action2
        val ctx = block.context
        when (type) {
            TextType.TEXT_PLAIN,
            TextType.LINE_CLICKABLE,
            TextType.WORD_CLICKABLE,
            TextType.HTML,
            TextType.LINK -> {
                action1.setText(R.string.clip_details_file_type_txt)
                action2.setText(R.string.clip_details_file_type_pdf)
                action1.setDebounceClickListener { confirmExport(ctx, R.string.clip_details_file_type_txt, FastAction.EXPORT_TO_TXT, FastAction.SEND_AS_TXT) }
                action2.setDebounceClickListener { confirmExport(ctx, R.string.clip_details_file_type_pdf, FastAction.EXPORT_TO_PDF, FastAction.SEND_AS_PDF) }
                action2.setVisibleOrGone(true)
            }
            TextType.QRCODE -> {
                action1.setText(R.string.clip_details_file_type_jpeg)
                action1.setDebounceClickListener { confirmExport(ctx, R.string.clip_details_file_type_jpeg, FastAction.EXPORT_TO_JPEG, FastAction.SEND_AS_JPEG) }
                action2.setVisibleOrGone(false)
            }
            TextType.MARKDOWN -> {
                action1.setText(R.string.clip_details_file_type_md)
                action2.setText(R.string.clip_details_file_type_pdf)
                action1.setDebounceClickListener { confirmExport(ctx, R.string.clip_details_file_type_md, FastAction.EXPORT_TO_MD, FastAction.SEND_AS_MD) }
                action2.setDebounceClickListener { confirmExport(ctx, R.string.clip_details_file_type_pdf, FastAction.EXPORT_TO_PDF, FastAction.SEND_AS_PDF) }
                action2.setVisibleOrGone(true)
            }
        }
    }

    private fun confirmExport(context: Context, extensionRes: Int, exportAction: FastAction, sendAction: FastAction) {
        val choices = arrayOf(
                context.getString(R.string.fast_actions_export_to_file),
                context.getString(R.string.fast_actions_send_as_file)
        )
        MaterialAlertDialogBuilder(context)
                .setTitle("${context.getString(R.string.clip_details_label_export)} ${context.getString(extensionRes)}")
                .setSingleChoiceItems(choices, -1) { dialog, which ->
                    when (which) {
                        0 -> viewModel.onFastAction(exportAction)
                        1 -> viewModel.onFastAction(sendAction)
                    }
                    dialog.dismiss()
                }
                .show()
    }

}