package com.example.minifrog.a3k

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.view.View


/**
 * Created by mini-frog on 2018/4/10.
 *
 * 分割线
 */

class MyDividerItemDecoration(context: Context, orientation: Int) : RecyclerView.ItemDecoration() {
    /**
     * 用于绘制间隔样式
     */
    private val mDivider: Drawable?
    /**
     * 列表的方向，水平/竖直
     */
    private var mOrientation: Int = 0


    init {
        // 获取默认主题的属性
        val a = context.obtainStyledAttributes(ATTRS)
        mDivider = a.getDrawable(0)
        a.recycle()
        setOrientation(orientation)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // 绘制间隔
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (mOrientation == VERTICAL_LIST) {
            outRect.set(0, 0, 0, mDivider!!.intrinsicHeight)
        } else {
            outRect.set(0, 0, mDivider!!.intrinsicWidth, 0)
        }
    }

    private fun setOrientation(orientation: Int) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw IllegalArgumentException("invalid orientation")
        }
        mOrientation = orientation
    }

    /**
     * 绘制间隔
     */
    private fun drawVertical(c: Canvas, parent: RecyclerView) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child
                    .layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin +
                    Math.round(ViewCompat.getTranslationY(child))
            val bottom = top + mDivider!!.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c)
        }
    }

    /**
     * 绘制间隔
     */
    private fun drawHorizontal(c: Canvas, parent: RecyclerView) {
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child
                    .layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin +
                    Math.round(ViewCompat.getTranslationX(child))
            val right = left + mDivider!!.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c)
        }
    }

    companion object {

        private val ATTRS = intArrayOf(android.R.attr.listDivider)
        val HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL
        val VERTICAL_LIST = LinearLayoutManager.VERTICAL
    }
}