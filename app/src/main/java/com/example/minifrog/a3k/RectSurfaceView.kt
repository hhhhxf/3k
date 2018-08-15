package com.example.minifrog.a3k

/**
 * Created by mini-frog on 2018/4/12.
 */

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView




class RectSurfaceView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var sh: SurfaceHolder = holder
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    init {
        // TODO Auto-generated constructor stub
        sh.addCallback(this)
        sh.setFormat(PixelFormat.TRANSPARENT)
        setZOrderOnTop(true)
    }

    override fun surfaceChanged(arg0: SurfaceHolder, arg1: Int, w: Int, h: Int) {
        // TODO Auto-generated method stub
        mWidth = w
        mHeight = h
    }

    override fun surfaceCreated(arg0: SurfaceHolder) {
        // TODO Auto-generated method stub

    }

    override fun surfaceDestroyed(arg0: SurfaceHolder) {
        // TODO Auto-generated method stub

    }

    internal fun clearDraw() {
        val canvas = sh.lockCanvas()
        canvas.drawColor(Color.BLUE)
        sh.unlockCanvasAndPost(canvas)
    }

    fun drawRect() {
        val canvas = sh.lockCanvas()
        canvas.drawColor(Color.TRANSPARENT)
        canvas.width
        val p = Paint()
        p.isAntiAlias = true
        p.color = Color.RED
        p.style = Style.STROKE
        p.strokeWidth = 3f
        //canvas.drawPoint(100.0f, 100.0f, p);
        val r = Rect(this.width / 2 - 300, this.height / 2- 55, this.width / 2 +300, this.height / 2+ 55)
        canvas.drawRect(r, p)
        sh.unlockCanvasAndPost(canvas)
    }

}