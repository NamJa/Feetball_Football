package com.chase1st.feetballfootball.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class BehaviorScoreTextView(private val context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<TextView>() {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: TextView,
        dependency: View
    ): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: TextView,
        dependency: View
    ): Boolean {
        child.x = ((dependency.width / 2) - (child.width / 2)).toFloat()
        if ((215.0f + (dependency.y*0.37f)) >= 11.0f)
            child.y = 215.0f + ((dependency.y*0.37f))

        if((dependency.height+dependency.y) < 150.0f) {
            child.y = 11.1f
        }

        return false
    }
}