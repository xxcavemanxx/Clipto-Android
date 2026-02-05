package clipto.presentation.blocks

import android.view.View
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.textfield.TextInputLayout
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_select_date_single.view.*

class ScanBarcodeBlock<C>(
    private val dialogState: DialogState,
    private val title: String? = null,
    private val value: String? = null,
    private val canRemove: Boolean,
    private val startScanning: Boolean,
    private val onValueChanged: (before: String?, after: String?) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_scan_barcode

    override fun areItemsTheSame(item: BlockItem<C>): Boolean {
        return super.areItemsTheSame(item) && item is ScanBarcodeBlock
                && startScanning == item.startScanning
    }

    override fun areContentsTheSame(item: BlockItem<C>): Boolean {
        return item is ScanBarcodeBlock
                && title == item.title
                && value == item.value
                && canRemove == item.canRemove
    }

    override fun onInit(context: C, block: View) {
        block as TextInputLayout

        block.setStartIconOnClickListener {
            val ref = block.tag
            if (ref is ScanBarcodeBlock<*>) {
                ref.startScanning()
            }
        }

        block.setEndIconOnClickListener {
            val ref = block.tag
            if (ref is ScanBarcodeBlock<*>) {
                ref.onValueChanged.invoke(ref.value, null)
            }
        }

    }

    override fun onBind(context: C, block: View) {
        block.tag = null
        block as TextInputLayout
        block.isEndIconVisible = canRemove
        block.tvEditText.setText(value)
        block.hint = title
        block.tag = this
        if (startScanning) {
            startScanning()
        }
    }

    private fun startScanning() {
        dialogState.requestScanBarcode {
            onValueChanged(value, it)
        }
    }

}