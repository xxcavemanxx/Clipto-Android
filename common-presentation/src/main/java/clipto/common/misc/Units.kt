package clipto.common.misc

import android.content.res.Resources
import android.util.DisplayMetrics

enum class Units {

    PX {
        override fun convert(sourceCount: Float, sourceUnit: Units): Float {
            return sourceUnit.toPx(sourceCount)
        }

        override fun toPx(count: Float): Float {
            return count
        }

        override fun toDp(count: Float): Float {
            return count / displayMetrics.density
        }

        override fun toSp(count: Float): Float {
            return count / displayMetrics.scaledDensity
        }
    },

    DP {

        override fun convert(sourceCount: Float, sourceUnit: Units): Float {
            return sourceUnit.toDp(sourceCount)
        }

        override fun toPx(count: Float): Float {
            return count * displayMetrics.density
        }

        override fun toDp(count: Float): Float {
            return count
        }

        override fun toSp(count: Float): Float {
            return count * (displayMetrics.scaledDensity / displayMetrics.density)
        }
    },

    SP {
        override fun convert(sourceCount: Float, sourceUnit: Units): Float {
            return sourceUnit.toSp(sourceCount)
        }

        override fun toPx(count: Float): Float {
            return count * displayMetrics.scaledDensity
        }

        override fun toDp(count: Float): Float {
            return count * (displayMetrics.density / displayMetrics.scaledDensity)
        }

        override fun toSp(count: Float): Float {
            return count
        }
    };

    abstract fun convert(sourceCount: Float, sourceUnit: Units): Float
    abstract fun toPx(count: Float): Float
    abstract fun toDp(count: Float): Float
    abstract fun toSp(count: Float): Float

    companion object {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        val toolbarHeightSm = DP.toPx(48f)
        val toolbarHeight = DP.toPx(56f)
    }
}