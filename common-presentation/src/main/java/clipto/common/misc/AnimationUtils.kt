package clipto.common.misc

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Vibrator
import android.view.View
import android.view.animation.DecelerateInterpolator
import clipto.common.extensions.gone
import clipto.common.extensions.visible

object AnimationUtils {

    private const val animationDurationFast: Int = 150

    fun show(view: View): Animator? {
        return scaleSet(view, 0.0f, 1.0f, object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.scaleX = 0.0f
                view.scaleY = 0.0f
                view.visible()
            }
        })
    }

    fun hide(view: View?): Animator? {
        return if (view == null) {
            null
        } else scaleSet(view, 1.0f, 0.0f, object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                view.visible()
            }

            override fun onAnimationEnd(animation: Animator) {
                view.gone()
            }
        })
    }

    fun shake(view: View, shakeCount: Int, vibrate: Boolean): Animator {
        val valueAnimator = ValueAnimator.ofFloat(-15f, 15f)
        valueAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            view.translationX = value
        }
        val duration = 50
        valueAnimator.duration = duration.toLong()
        valueAnimator.repeatCount = shakeCount
        valueAnimator.repeatMode = ValueAnimator.REVERSE
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                if (vibrate) {
                    vibrate(view.context, duration.toLong())
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                view.translationX = 0f
            }
        })
        return valueAnimator
    }

    private fun scaleSet(
            view: View?,
            scaleFrom: Float,
            scaleTo: Float,
            listener: Animator.AnimatorListener?
    ): Animator? {
        if (view == null) {
            return null
        }
        val scaleAnimator = ValueAnimator.ofFloat(scaleFrom, scaleTo)
        scaleAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            view.scaleX = value
            view.scaleY = value
        }
        scaleAnimator.interpolator = DecelerateInterpolator()
        scaleAnimator.duration = animationDurationFast.toLong()
        if (listener != null) {
            scaleAnimator.addListener(listener)
        }
        return scaleAnimator
    }

    fun translationY(
            view: View?,
            from: Float,
            to: Float,
            listener: Animator.AnimatorListener?
    ): Animator? {
        if (view == null) {
            return null
        }
        val animator = ValueAnimator.ofFloat(from, to)
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            view.translationY = value
        }
        animator.interpolator = DecelerateInterpolator()
        animator.duration = animationDurationFast.toLong()
        if (listener != null) {
            animator.addListener(listener)
        }
        return animator
    }

    fun vibrate(context: Context, milliseconds: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(milliseconds)
        }
    }
}