package clipto.presentation.blocks.domain

import android.view.View
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import clipto.common.extensions.hideKeyboard
import clipto.common.extensions.notNull
import clipto.domain.ClientSession
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_separators.view.*

class SeparatorsBlock<C>(
    val textSeparator: String,
    val onChanged: (separator: String) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_separators

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is SeparatorsBlock && textSeparator == item.textSeparator

    override fun onInit(context: C, block: View) {
        val ctx = block.context

        val separators = mutableListOf<Separator>()
        separators.add(
            Separator(
                ctx.getString(R.string.clip_multiple_merge_separator_line),
                ClientSession.SEPARATOR_NEW_LINE
            )
        )
        separators.add(
            Separator(
                ctx.getString(R.string.clip_multiple_merge_separator_space),
                ClientSession.SEPARATOR_SPACE
            )
        )
        separators.add(
            Separator(
                ClientSession.SEPARATOR_MD.trim(),
                ClientSession.SEPARATOR_MD
            )
        )

        separators.forEach { separator ->
            val chip = View.inflate(ctx, R.layout.chip_separator, null) as Chip
            chip.text = separator.title
            chip.tag = separator
            chip.setOnClickListener {
                block.etCustom.hideKeyboard()
                getRef(block)?.onChanged?.invoke(separator.value)
                block.etCustom.clearFocus()
                updateState(chip, true)
            }
            updateState(chip, separator.value == textSeparator)
            block.separatorView.addView(chip)
        }
        block.etCustom.doAfterTextChanged {
            if (it === block.etCustom.text) {
                getRef(block)?.onChanged?.invoke(it?.toString().notNull())
            }
        }
        block.etCustom.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                getRef(block)?.onChanged?.invoke(block.etCustom.text?.toString().notNull())
                block.separatorView.clearCheck()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        if (block.tag != null) return
        var standard = false
        block.separatorView
            .children
            .filter { it is Chip }
            .map { it as Chip }
            .forEach { chip ->
                val tag = chip.tag
                val isChecked = tag is Separator && tag.value == textSeparator
                standard = standard || isChecked
                updateState(chip, isChecked)
            }
        if (!standard) {
            block.etCustom.setText(textSeparator)
        }
        block.tag = this
    }

    private fun updateState(chip: Chip, isChecked: Boolean) {
        chip.isChecked = isChecked
    }

    private fun getRef(block: View): SeparatorsBlock<*>? {
        val ref = block.tag
        if (ref is SeparatorsBlock<*>) {
            return ref
        }
        return null
    }

    data class Separator(
        val title: CharSequence,
        val value: String
    )
}