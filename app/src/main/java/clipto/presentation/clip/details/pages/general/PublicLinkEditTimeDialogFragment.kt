package clipto.presentation.clip.details.pages.general
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentClipPublicLinkEditTimeBinding
import android.view.Gravity
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doOnTextChanged
import clipto.common.extensions.animateScale
import clipto.common.extensions.withSafeFragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
abstract class PublicLinkEditTimeDialogFragment : PublicLinkEditDialogFragment() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClipPublicLinkEditTimeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentClipPublicLinkEditTimeBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_clip_public_link_edit_time

    override fun bind(viewModel: GeneralPageViewModel) {
        val dateFormat = SimpleDateFormat(getString(R.string.common_mask_date), Locale.ROOT)
        var timeAsDate: Calendar? = getInitialTimeAsDate()?.let { date -> Calendar.getInstance().apply { time = date } }
        var timeInMillis: Long? = getInitialTimeInMillis()
        var timeOption = TimeOption.byMillis(timeInMillis)

        // icon
        binding.iconView.setImageResource(getIconRes())
        // title
        binding.titleView.setText(getTitleRes())
        // description
        binding.descriptionView.setText(getDescriptionRes())
        // time label
        binding.timeLabel.setText(getTimeLabelRes())
        // time value
        binding.timeValueView.hint = "0"
        binding.timeValueView.setText(timeInMillis?.let { it / timeOption.value }?.toString())
        binding.timeValueView.doOnTextChanged { text, _, _, _ ->
            val value = text?.toString()?.toIntOrNull()
            timeInMillis = value?.let { it * timeOption.value }
            timeAsDate = null
        }
        // time type
        binding.timeValueTypeView.setText(timeOption.titleRes)
        binding.timeValueTypeView.setOnClickListener {
            val popupMenu = PopupMenu(it.context, it, Gravity.CENTER)
            popupMenu.menu.apply {
                TimeOption.values().forEach { option ->
                    add(0, option.id, option.id, option.titleRes)
                }
            }
            popupMenu.setOnMenuItemClickListener { item ->
                timeOption = TimeOption.byId(item.itemId)
                binding.timeValueTypeView?.setText(timeOption.titleRes)
                val timeValue = binding.timeValueView?.text?.toString()?.toIntOrNull()
                if (timeValue != null) {
                    timeInMillis = timeValue * timeOption.value
                    timeAsDate = null
                }
                true
            }
            popupMenu.show()
        }

        // date value
        binding.dateView.text = timeAsDate?.let { dateFormat.format(it.time) }
        binding.dateView.setOnClickListener {
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
                    newDate.set(Calendar.HOUR_OF_DAY, binding.dateHoursView.text.toString().toIntOrNull()
                            ?: 0)
                    newDate.set(Calendar.MINUTE, binding.dateMinutesView.text.toString().toIntOrNull() ?: 0)
                    binding.dateView?.text = dateFormat.format(newDate.time)
                    timeAsDate = newDate
                    timeInMillis = null
                }
                picker.show(fm, "DatePicker")
            }
        }

        // hours value
        binding.dateHoursView.text = formatTime(timeAsDate?.get(Calendar.HOUR_OF_DAY))
        binding.dateHoursView.setOnClickListener {
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
                binding.dateView.text = calendar.let { dateFormat.format(it.time) }
                binding.dateHoursView?.text = formatTime(hour)
                timeAsDate = calendar
                timeInMillis = null
                true
            }
            popupMenu.show()
        }

        // minutes value
        binding.dateMinutesView.text = formatTime(timeAsDate?.get(Calendar.MINUTE))
        binding.dateMinutesView.setOnClickListener {
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
                binding.dateView.text = calendar.let { dateFormat.format(it.time) }
                binding.dateMinutesView?.text = formatTime(minute)
                timeAsDate = calendar
                timeInMillis = null
                true
            }
            popupMenu.show()
        }

        // cancel action
        binding.cancelAction.setOnClickListener { dismissAllowingStateLoss() }

        // apply action
        binding.applyAction.setOnClickListener {
            onApply(timeInMillis, timeAsDate?.time)
        }

        binding.iconView.animateScale(true)
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
