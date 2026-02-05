package clipto.presentation.common.dialog.select.date

import clipto.common.misc.AndroidUtils
import clipto.domain.TimePeriod
import java.util.*

data class SelectDateDialogRequest(
    val id: Int = AndroidUtils.nextId(),
    val title: CharSequence,
    var selection: Selection,
    val timePeriods: List<TimePeriod> = TimePeriod.periods.toList(),
    val onSelected: (selection: Selection?) -> Unit,
    val withImmediateNotify: Boolean = false
) {

    internal var options: List<Option> = emptyList()

    fun changeSelection(selection: Selection) {
        this.selection = selection
        if (withImmediateNotify) {
            onSelected(selection.normalize())
        }
    }

    data class Option(
        val model: TimePeriod,
        val checked: Boolean,
        val title: CharSequence
    )

    data class Selection(
        val model: TimePeriod? = null,
        val dateFrom: Date? = null,
        val dateTo: Date? = null
    ) {
        fun normalize(): Selection {
            return when {
                model == TimePeriod.CUSTOM_INTERVAL && dateFrom == null && dateTo == null -> Selection()
                else -> this
            }
        }
    }
}