package clipto.presentation.common.fragment.attributed.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.setVisibleOrGone
import clipto.domain.AttributedObject
import clipto.domain.AttributedObjectScreenState
import clipto.domain.FocusMode
import clipto.extensions.log
import clipto.presentation.common.fragment.attributed.AttributedObjectViewModel
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.widget.AutoCompleteItem
import clipto.store.StoreObject
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_attributed_object_add_tag.view.*

class AddTagBlock<O : AttributedObject, S : AttributedObjectScreenState<O>>(
    private val viewModel: AttributedObjectViewModel<O, S>,
    private val screenState: S,
    private val tagsLive: StoreObject<List<String>>,
    private val onAdded: (name: CharSequence?) -> Unit
) : BlockItem<Fragment>() {

    override val layoutRes: Int = R.layout.block_attributed_object_add_tag

    override fun areContentsTheSame(item: BlockItem<Fragment>): Boolean =
        item is AddTagBlock<*, *> && item.screenState.focusMode != FocusMode.TAGS

    override fun onInit(fragment: Fragment, block: View) {
        val tagsEditTextActionButton = block.tagsEditTextActionButton
        val tagsEditText = block.tagsEditText
        val appConfig = viewModel.appConfig
        val context = block.context
        val onAddTag: (name: CharSequence?) -> Unit = { name ->
            val ref = block.tag
            if (ref is AddTagBlock<*, *>) {
                ref.onAdded(name)
            }
        }

        tagsEditText
            .withInputMaxLength(appConfig.maxLengthTag())
            .withSelectedItemsProvider { tagsLive.getValue() ?: emptyList() }
            .withAllItemsProvider { viewModel.appState.getFilters().getSortedTags().map { AutoCompleteItem(it.uid, it.name) } }
            .withOnTextChangeListener { s ->
                val isEmpty = s.isNullOrBlank()
                tagsEditTextActionButton.setVisibleOrGone(!isEmpty)
                tagsEditText.hint =
                    if (isEmpty) {
                        context.getString(R.string.clip_hint_tags)
                    } else {
                        null
                    }
            }
            .withOnItemClickListener { onAddTag(it) }
            .withOnEnterListener {
                onAddTag(it)
                if (it.isNullOrBlank()) {
                    viewModel.onNextFocus(FocusMode.TEXT)
                }
            }

        tagsEditTextActionButton.setOnClickListener {
            val text = tagsEditText.text
            tagsEditText.text = null
            onAddTag(text)
            tagsEditText.showResults()
        }
    }

    override fun onBind(fragment: Fragment, block: View) {
        val tagsEditText = block.tagsEditText
        block.tag = this

        log("onBind :: {}", screenState.focusMode)

        tagsEditText.text = null

        when (screenState.focusMode) {
            FocusMode.TAGS -> tagsEditText.requestFocus()
            FocusMode.PREVIEW -> Unit
            else -> tagsEditText.clearFocus()
        }
    }

}