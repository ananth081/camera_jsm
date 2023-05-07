package com.android.example.cameraxbasic.extensions

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.DecelerateInterpolator

fun View.animateXTranslation(
    transValFrom: Float,
    transValTo: Float,
    duration: Long,
    onAnimEnded: (() -> Unit)? = null
) {
    val translateXPH = PropertyValuesHolder.ofFloat(View.X, transValFrom, transValTo)

    val yAnimator = ObjectAnimator.ofPropertyValuesHolder(this, translateXPH).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        this.animationCallBacks(onAnimEndCallback = {
            onAnimEnded?.invoke()
            removeAllListeners()
        })
    }
    yAnimator.start()
}

fun ObjectAnimator.animationCallBacks(
    onAnimStartedCallback: (() -> Unit)? = null,
    onAnimEndCallback: (() -> Unit)? = null,
    onRepeatAnimation: (() -> Unit)? = null
) {
    this.addListener(object : Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator) {
            onAnimStartedCallback?.invoke()
        }

        override fun onAnimationEnd(animation: Animator) {
            onAnimEndCallback?.invoke()
        }

        override fun onAnimationCancel(animation: Animator) = Unit

        override fun onAnimationRepeat(animation: Animator) {
            onRepeatAnimation?.invoke()
        }
    })
}
