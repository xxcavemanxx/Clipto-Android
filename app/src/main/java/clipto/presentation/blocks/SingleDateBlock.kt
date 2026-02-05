package clipto.presentation.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.misc.FormatUtils
import clipto.extensions.getMaterialTimeFormatStyle
import clipto.extensions.withDate
import clipto.extensions.withUtc
import clipto.extensions.withoutTime
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_select_date_single.view.*
import java.util.*

class SingleDateBlock<F : Fragment>(
    private val title: String? = null,
    private val format: String? = null,
    private val currentDate: () -> Date?,
    private val onDateChanged: (date: Date) -> Unit,
    private val date: Date? = currentDate.invoke()
) : BlockItem<F>() {

    override val layoutRes: Int = R.layout.block_select_date_single

    override fun areContentsTheSame(item: BlockItem<F>): Boolean {
        return item is SingleDateBlock
                && date == item.date
                && format == item.format
                && title == item.title
    }

    override fun onInit(context: F, block: View) {
        block as TextInputLayout

        block.setStartIconOnClickListener {
            val ref = block.tag as SingleDateBlock<*>
            val currentDate = ref.currentDate.invoke()
            val calendar = Calendar.getInstance().withDate(currentDate?.time ?: System.currentTimeMillis()).withUtc()
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(calendar.timeInMillis)
                .build()
            picker.addOnPositiveButtonClickListener {
                val millis = picker.selection ?: System.currentTimeMillis()
                val newDate = Calendar.getInstance().withDate(millis).withoutTime()
                val prevDate = currentDate?.let { Calendar.getInstance().withDate(it.time) }
                newDate.set(Calendar.HOUR_OF_DAY, prevDate?.get(Calendar.HOUR_OF_DAY) ?: 0)
                newDate.set(Calendar.MINUTE, prevDate?.get(Calendar.MINUTE) ?: 0)
                ref.onDateChanged.invoke(newDate.time)
                onBind(context, block)
            }
            picker.show(context.parentFragmentManager, "DatePicker")
        }

        block.setEndIconOnClickListener {
            val ref = block.tag as SingleDateBlock<*>
            val currentDate = ref.currentDate.invoke()
            val calendar = Calendar.getInstance().withDate(currentDate?.time ?: System.currentTimeMillis())
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
                ref.onDateChanged.invoke(calendar.time)
                onBind(context, block)
            }
            picker.show(context.parentFragmentManager, "TimePicker")
        }
    }

    override fun onBind(context: F, block: View) {
        block.tag = this
        block as TextInputLayout
        val pattern = format ?: FormatUtils.getDateTimeShortPattern()
        val value = FormatUtils.formatDate(currentDate.invoke(), pattern)
        block.tvEditText.setText(value)
        block.hint = title ?: pattern
    }

}