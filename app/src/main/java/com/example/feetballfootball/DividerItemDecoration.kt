package com.example.feetballfootball

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(var dividerTransparent: Drawable) : RecyclerView.ItemDecoration() {

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerLeft: Int = parent.paddingLeft
        val dividerRight: Int = parent.width - parent.paddingRight
        val childCount: Int = parent.childCount
        for (i in 0 until childCount) {
            val child: View = parent.getChildAt(i)
            val params: RecyclerView.LayoutParams = child.layoutParams as RecyclerView.LayoutParams
            val dividerTop = child.bottom+params.bottomMargin
            val dividerBottom = dividerTop+dividerTransparent.intrinsicHeight
            dividerTransparent.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
            dividerTransparent.draw(canvas)
        }
    }
}