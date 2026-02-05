package clipto.presentation.common.widget

import android.os.Parcel
import android.text.TextPaint
import android.text.style.ForegroundColorSpan

open class ColorfulTagSpan : ForegroundColorSpan {

    var tagColor: Int
    var textColor: Int
    val tagId: String

    constructor(tagId: String, tagColor: Int = 0, textColor: Int = 0) : super(textColor) {
        this.tagColor = tagColor
        this.textColor = textColor
        this.tagId = tagId
    }

    constructor(parcel: Parcel) : super(parcel) {
        this.tagColor = parcel.readInt()
        this.textColor = parcel.readInt()
        this.tagId = parcel.readString() ?: ""
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(tagColor)
        dest.writeInt(textColor)
        dest.writeString(tagId)
    }

    override fun getForegroundColor(): Int = textColor

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.color = textColor
    }

}

class ColorfulIconTagSpan(tagName: String, tagColor: Int = 0, textColor: Int) : ColorfulTagSpan(tagName, tagColor, textColor)