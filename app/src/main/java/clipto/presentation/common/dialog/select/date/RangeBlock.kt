package clipto.presentation.common.dialog.select.date

import android.view.View
import androidx.lifecycle.MutableLiveData
import clipto.common.misc.FormatUtils
import clipto.extensions.getMaterialTimeFormatStyle
import clipto.extensions.withDate
import clipto.extensions.withUtc
import clipto.extensions.withoutTime
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_dialog_select_date_range.view.*
import java.util.*

class RangeBlock(
    val live: MutableLiveData<List<BlockItem<SelectDateDialogFragment>>>,
    val data: SelectDateDialogRequest
) : BlockItem<SelectDateDialogFragment>() {

    override val layoutRes: Int = R.layout.block_dialog_select_date_range

    override fun areContentsTheSame(item: BlockItem<SelectDateDialogFragment>): Boolean {
        return item is RangeBlock &&
                item.data.selection.dateFrom == data.selection.dateFrom &&
                item.data.selection.dateTo == data.selection.dateTo
    }

    override fun onInit(fragment: SelectDateDialogFragment, block: View) {
        block.tilFrom.setStartIconOnClickListener {
            val ref = block.tag as RangeBlock
            val selection = ref.data.selection
            val calendar = Calendar.getInstance().withDate(selection.dateFrom?.time ?: System.currentTimeMillis()).withUtc()
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(calendar.timeInMillis)
                .build()
            picker.addOnPositiveButtonClickListener {
                val millis = picker.selection ?: System.currentTimeMillis()
                val newDate = Calendar.getInstance().withDate(millis).withoutTime()
                val prevDate = selection.dateFrom?.let { Calendar.getInstance().withDate(it.time) }
                newDate.set(Calendar.HOUR_OF_DAY, prevDate?.get(Calendar.HOUR_OF_DAY) ?: 0)
                newDate.set(Calendar.MINUTE, prevDate?.get(Calendar.MINUTE) ?: 0)
                ref.data.changeSelection(ref.data.selection.copy(dateFrom = newDate.time))
                onBind(fragment, block)
            }
            picker.show(fragment.parentFragmentManager, "DatePicker")
        }

        block.tilFrom.setEndIconOnClickListener {
            val ref = block.tag as RangeBlock
            val selection = ref.data.selection
            val calendar = Calendar.getInstance().withDate(selection.dateFrom?.time ?: System.currentTimeMillis())
            val picker = MaterialTimePicker.Builder()
                .setMinute(calendar.get(Calendar.MINUTE))
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setTimeFormat(block.context.getMaterialTimeFormatStyle())
                .build()
            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                ref.data.changeSelection(ref.data.selection.copy(dateFrom = calendar.time))
                onBind(fragment, block)
            }
            picker.show(fragment.parentFragmentManager, "TimePicker")
        }

        block.tilTo.setStartIconOnClickListener {
            val ref = block.tag as RangeBlock
            val selection = ref.data.selection
            val calendar = Calendar.getInstance().withDate(selection.dateTo?.time ?: System.currentTimeMillis()).withUtc()
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(calendar.timeInMillis)
                .build()
            picker.addOnPositiveButtonClickListener {
                val millis = picker.selection ?: System.currentTimeMillis()
                val newDate = Calendar.getInstance().withDate(millis)
                val prevDate = selection.dateTo?.let { Calendar.getInstance().withDate(it.time) }
                newDate.set(Calendar.HOUR_OF_DAY, prevDate?.get(Calendar.HOUR_OF_DAY) ?: 0)
                newDate.set(Calendar.MINUTE, prevDate?.get(Calendar.MINUTE) ?: 0)
                newDate.set(Calendar.SECOND, 0)
                ref.data.changeSelection(ref.data.selection.copy(dateTo = newDate.time))
                onBind(fragment, block)
            }
            picker.show(fragment.parentFragmentManager, "DatePicker")
        }

        block.tilTo.setEndIconOnClickListener {
            val ref = block.tag as RangeBlock
            val selection = ref.data.selection
            val calendar = Calendar.getInstance().withDate(selection.dateTo?.time ?: System.currentTimeMillis())
            val picker = MaterialTimePicker.Builder()
                .setMinute(calendar.get(Calendar.MINUTE))
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setTimeFormat(block.context.getMaterialTimeFormatStyle())
                .build()
            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                ref.data.changeSelection(ref.data.selection.copy(dateTo = calendar.time))
                onBind(fragment, block)
            }
            picker.show(fragment.parentFragmentManager, "TimePicker")
        }
    }

    override fun onBind(fragment: SelectDateDialogFragment, block: View) {
        block.tag = this
        block.tvFrom.setText(data.selection.dateFrom?.let { FormatUtils.formatDateTimeShort(it) })
        block.tvTo.setText(data.selection.dateTo?.let { FormatUtils.formatDateTimeShort(it) })
    }

}