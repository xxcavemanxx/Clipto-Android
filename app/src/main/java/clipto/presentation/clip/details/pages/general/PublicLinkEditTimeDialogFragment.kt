package clipto.presentation.clip.details.pages.general

import android.view.Gravity
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doOnTextChanged
import clipto.common.extensions.animateScale
import clipto.common.extensions.withSafeFragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_clip_public_link_edit_time.*
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
abstract class PublicLinkEditTimeDialogFragment : PublicLinkEditDialogFragment() {

    override val layoutResId: Int = R.layout.fragment_clip_public_link_edit_time

    override fun bind(viewModel: GeneralPageViewModel) {
        val dateFormat = SimpleDateFormat(getString(R.string.common_mask_date), Locale.ROOT)
        var timeAsDate: Calendar? = getInitialTimeAsDate()?.let { date -> Calendar.getInstance().apply { time = date } }
        var timeInMillis: Long? = getInitialTimeInMillis()
        var timeOption = TimeOption.byMillis(timeInMillis)

        // icon
        iconView.setImageResource(getIconRes())
        // title
        titleView.setText(getTitleRes())
        // description
        descriptionView.setText(getDescriptionRes())
        // time label
        timeLabel.setText(getTimeLabelRes())
        // time value
        timeValueView.hint = "0"
        timeValueView.setText(timeInMillis?.let { it / timeOption.value }?.toString())
        timeValueView.doOnTextChanged { text, _, _, _ ->
            val value = text?.toString()?.toIntOrNull()
            timeInMillis = value?.let { it * timeOption.value }
            timeAsDate = null
        }
        // time type
        timeValueTypeView.setText(timeOption.titleRes)
        timeValueTypeView.setOnClickListener {
            val popupMenu = PopupMenu(it.context, it, Gravity.CENTER)
            popupMenu.menu.apply {
                TimeOption.values().forEach { option ->
                    add(0, option.id, option.id, option.titleRes)
                }
            }
            popupMenu.setOnMenuItemClickListener { item ->
                timeOption = TimeOption.byId(item.itemId)
                timeValueTypeView?.setText(timeOption.titleRes)
                val timeValue = timeValueView?.text?.toString()?.toIntOrNull()
                if (timeValue != null) {
                    timeInMillis = timeValue * timeOption.value
                    timeAsDate = null
                }
                true
            }
            popupMenu.show()
        }

        // date value
        dateView.text = timeAsDate?.let { dateFormat.format(it.time) }
        dateView.setOnClickListener {
            withSafeFragmentManager()?.let { fm ->
                val startAt = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
                val calendar = timeAsDate ?: Calendar.getInstance()
                val picker = MaterialDatePicker.Builder.datePicker()
                        .setCalendarConstraints(CalendarConstraints.Builder().setStart(startAt).build())
                        .setSelection(calendar.timeInMillis)
                        .build()
                picker.addOnPositiveButtonClickListener {
                    val millis = picker.selection ?: System.currentTimeMillis()
                    val newDate = Calendar.getInstance().apply {
                        setTimeInMillis(millis)
                    }
                    newDate.set(Calendar.HOUR_OF_DAY, dateHoursView.text.toString().toIntOrNull()
                            ?: 0)
                    newDate.set(Calendar.MINUTE, dateMinutesView.text.toString().toIntOrNull() ?: 0)
                    dateView?.text = dateFormat.format(newDate.time)
                    timeAsDate = newDate
                    timeInMillis = null
                }
                picker.show(fm, "DatePicker")
            }
        }

        // hours value
        dateHoursView.text = formatTime(timeAsDate?.get(Calendar.HOUR_OF_DAY))
        dateHoursView.setOnClickListener {
            val popupMenu = PopupMenu(it.context, it, Gravity.CENTER)
            popupMenu.menu.apply {
                (0..23).forEach { hour ->
                    add(0, hour, hour, formatTime(hour))
                }
            }
            popupMenu.setOnMenuItemClickListener { item ->
                val hour = item.itemId
                val calendar = timeAsDate ?: Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                dateView.text = calendar.let { dateFormat.format(it.time) }
                dateHoursView?.text = formatTime(hour)
                timeAsDate = calendar
                timeInMillis = null
                true
            }
            popupMenu.show()
        }

        // minutes value
        dateMinutesView.text = formatTime(timeAsDate?.get(Calendar.MINUTE))
        dateMinutesView.setOnClickListener {
            val popupMenu = PopupMenu(it.context, it, Gravity.CENTER)
            popupMenu.menu.apply {
                (0..59).forEach { minute ->
                    add(0, minute, minute, formatTime(minute))
                }
            }
            popupMenu.setOnMenuItemClickListener { item ->
                val minute = item.itemId
                val calendar = timeAsDate ?: Calendar.getInstance()
                calendar.set(Calendar.MINUTE, minute)
                dateView.text = calendar.let { dateFormat.format(it.time) }
                dateMinutesView?.text = formatTime(minute)
                timeAsDate = calendar
                timeInMillis = null
                true
            }
            popupMenu.show()
        }

        // cancel action
        cancelAction.setOnClickListener { dismissAllowingStateLoss() }

        // apply action
        applyAction.setOnClickListener {
            onApply(timeInMillis, timeAsDate?.time)
        }

        iconView.animateScale(true)
    }

    private fun formatTime(time: Int?): String {
        if (time == null) {
            return "00"
        }
        if (time < 10) {
            return "0${time}"
        }
        return time.toString()
    }

    protected abstract fun getIconRes(): Int
    protected abstract fun getTitleRes(): Int
    protected abstract fun getDescriptionRes(): Int
    protected abstract fun getTimeLabelRes(): Int
    protected abstract fun getInitialTimeAsDate(): Date?
    protected abstract fun getInitialTimeInMillis(): Long?
    protected abstract fun onApply(timeInMillis: Long?, timeAsDate: Date?)

    enum class TimeOption(val id: Int, val titleRes: Int, val value: Long) {
        SECONDS(1, R.string.public_note_link_time_type_seconds, 1000),
        MINUTES(2, R.string.public_note_link_time_type_minutes, 60 * 1000),
        HOURS(3, R.string.public_note_link_time_type_hours, 60 * 60 * 1000),
        DAYS(4, R.string.public_note_link_time_type_days, 24 * 60 * 60 * 1000);

        companion object {
            fun byId(id: Int): TimeOption = when (id) {
                SECONDS.id -> SECONDS
                MINUTES.id -> MINUTES
                HOURS.id -> HOURS
                DAYS.id -> DAYS
                else -> HOURS
            }

            fun byMillis(millis: Long?): TimeOption {
                if (millis == null) {
                    return HOURS
                }
                return values().reversed().find { millis % it.value == 0L } ?: HOURS
            }
        }
    }

}