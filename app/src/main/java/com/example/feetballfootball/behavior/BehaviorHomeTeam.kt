package com.example.feetballfootball.behavior

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class BehaviorHomeTeam(private var context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<ImageView>() {


    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: ImageView,
        dependency: View
    ): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: ImageView,
        dependency: View
    ): Boolean {
        var ratio = ((dependency.height+dependency.y) / dependency.height)
        if (ratio > 0.45) {
            child.scaleX = ratio
            child.scaleY = ratio
        }
        if ((170.0f + (dependency.y*0.37f)) > -28.0f) {
            child.y = 170.0f + (dependency.y*0.37f)
        }
        // 너무 빨리 스크롤을 하게 되면 연산에 병목이 생겨 UI가 밀리는데, 이 때를 대비하기 위한 코드
        if((dependency.height+dependency.y) < 150.0f) {
            child.y = -28.0f
            child.scaleX = 0.45f
            child.scaleY = 0.45f
        }
//        Log.d("yasyasyas", (dependency.height+dependency.y).toString())
        val marginLeft = (dependency.width / 2)/8.toFloat()
        child.x = marginLeft-(dependency.y*0.2f)
        return false
    }

    private fun dpToPx(dp: Float): Float {
        val dm: DisplayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm).toFloat()
    }

}