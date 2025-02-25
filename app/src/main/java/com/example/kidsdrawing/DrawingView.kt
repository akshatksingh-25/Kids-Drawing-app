package com.example.kidsdrawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View



class DrawingView(context: Context, attrs : AttributeSet) : View(context,attrs) {

    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap : Bitmap? = null
    private var mDrawPaint : Paint? = null
    private var mCanvasPaint : Paint? = null
    private var mBrushSize : Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas : Canvas? = null

    private var mPaths = ArrayList<CustomPath>() //Array List for Paths
    private var mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }


    fun setEraser() {
        color = Color.WHITE // Set color to transparent for erasing
        mDrawPaint!!.color = color // Update the paint color
    }
    fun setEraserSize(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize // Update the paint stroke width
    }



    fun onClickRedo(){
        if(mUndoPaths.size>0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }


    fun onClickUndo(){
        if(mPaths.size > 0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
            //In your onClickUndo() function, the invalidate() method is used to request a redraw of the view.
            // When you call invalidate(), it tells Android that the view's appearance has changed,
            // so the system will schedule a call to onDraw() where the view is redrawn on the screen.
        }
    }



    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //1 to persist view
//        mBrushSize = 20.toFloat() -- No need because we are setting accordingly
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }


    // Change Canvas to Canvas? if it fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(mCanvasBitmap!!,0f,0f,mDrawPaint)

        //3 to persist view
        for (path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path,mDrawPaint!!)
        }
        //

        if(!mDrawPath!!.isEmpty){
            //Here we set how thick the paint
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){

            MotionEvent.ACTION_DOWN -> {
                //Here we set how thick the path
                mDrawPath!!.color=color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                //2 to persist view
                mPaths.add(mDrawPath!!)
                //
                mDrawPath = CustomPath(color,mBrushSize)
            }

            else -> {
                return false
            }
        }
        invalidate()

        return true
    }



    fun setSizeForBrush(newSize : Float){
        // Here different screens will show different thickness of a given value so we just change it to
        // change according to DIP that's why we use TypedValue here.
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }


    fun setColor(newColor : String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }


    internal inner class CustomPath(
        var color: Int,
        var brushThickness: Float,
    ) : Path()

}