package com.gmail.weronikapios7.drawit

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var drawPath : CustomPath? = null
    private var canvasBitmap: Bitmap? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var brushSize: Float = 0F
    private var color = Color.BLACK //brush color
    private var canvas: Canvas? = null // canvas to draw on
    private val paths = ArrayList<CustomPath>()

    init{
        setUpDrawing()
    }

    private fun setUpDrawing(){
        drawPaint = Paint()
        drawPath = CustomPath(color,brushSize)
        drawPaint!!.color = color
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
        brushSize = 20F
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        for(path in paths){
            drawPaint!!.strokeWidth = path.brushThickness
            drawPaint!!.color = path.color
            canvas?.drawPath(path, drawPaint!!)
        }

        drawPaint.let{
            it?.strokeWidth = drawPath!!.brushThickness
            it?.color = drawPath!!.color
            canvas?.drawPath(drawPath!!, drawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = color
                drawPath!!.brushThickness = brushSize

                drawPath!!.reset()
                if (touchY != null && touchX != null) {
                    drawPath!!.moveTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null && touchY != null) {
                    drawPath!!. lineTo(touchX, touchY)
                }
            }

            MotionEvent.ACTION_UP ->{
                paths.add(drawPath!!)
                drawPath = CustomPath(color, brushSize)
            }
            else -> return false
        }

        invalidate()

        return true
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {

    }
}