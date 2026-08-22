package com.prismspace.container.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.prismspace.container.util.MathUtil

class RockerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), Runnable, SurfaceHolder.Callback {

    private lateinit var mHolder: SurfaceHolder
    private lateinit var mPaint: Paint

    private lateinit var mAreaPosition: Point
    private lateinit var mRockerPosition: Point

    private var mAreaRadius = -1
    private var mRockerRadius = -1

    private var mAreaColor = DEFAULT_AREA_COLOR
    private var mRockerColor = DEFAULT_ROCKER_COLOR
    private var mAreaBitmap: Bitmap? = null
    private var mRockerBitmap: Bitmap? = null

    private var canMove = true

    private var mListener: RockerListener? = null

    private var mRefreshCycle = DEFAULT_REFRESH_CYCLE
    private var mCallbackCycle = DEFAULT_CALLBACK_CYCLE

    init {
        initAttrs(context, attrs)
        setPaint()

        if (!isInEditMode) {
            configSurfaceView()
            configSurfaceHolder()
        }
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        mAreaColor = DEFAULT_AREA_COLOR
        mRockerColor = DEFAULT_ROCKER_COLOR
        mAreaRadius = DEFAULT_AREA_RADIUS
        mRockerRadius = DEFAULT_ROCKER_RADIUS
    }

    private fun setPaint() {
        mPaint = Paint()
        mPaint.isAntiAlias = true
    }

    private fun configSurfaceView() {
        keepScreenOn = true
        isFocusable = true
        isFocusableInTouchMode = true
        setZOrderOnTop(true)
    }

    private fun configSurfaceHolder() {
        mHolder = holder
        mHolder.addCallback(this)
        mHolder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultWidth = (mAreaRadius + mRockerRadius) * 2
        val defaultHeight = defaultWidth

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val measureWidth = if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED || widthSize < 0) {
            defaultWidth
        } else {
            widthSize
        }

        val measureHeight = if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED || heightSize < 0) {
            defaultHeight
        } else {
            heightSize
        }

        setMeasuredDimension(measureWidth, measureHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mAreaPosition = Point(w / 2, h / 2)
        mRockerPosition = Point(mAreaPosition)

        var tempRadius = minOf(
            w - paddingLeft - paddingRight,
            h - paddingTop - paddingBottom
        )
        tempRadius /= 2
        if (mAreaRadius == -1) {
            mAreaRadius = (tempRadius * 0.75f).toInt()
        }
        if (mRockerRadius == -1) {
            mRockerRadius = (tempRadius * 0.25f).toInt()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            mDrawThread = Thread(this)
            mDrawThread?.start()

            mCallbackThread = Thread {
                while (mCallbackOk) {
                    listenerCallback()
                    try {
                        Thread.sleep(mCallbackCycle.toLong())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            mCallbackThread?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mDrawOk = false
        mCallbackOk = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            mDrawOk = true
            mCallbackOk = true
        } else {
            mDrawOk = false
            mCallbackOk = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::mAreaPosition.isInitialized || !::mRockerPosition.isInitialized) {
            return true
        }

        try {
            val len = MathUtil.getDistance(
                mAreaPosition.x.toFloat(),
                mAreaPosition.y.toFloat(),
                event.x,
                event.y
            )

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (len > mAreaRadius) {
                    return true
                }
            }

            if (event.action == MotionEvent.ACTION_MOVE) {
                if (len <= mAreaRadius) {
                    mRockerPosition.set(event.x.toInt(), event.y.toInt())
                } else {
                    mRockerPosition = MathUtil.getPointByCutLength(
                        mAreaPosition,
                        Point(event.x.toInt(), event.y.toInt()),
                        mAreaRadius
                    )
                }
                mListener?.let { listener ->
                    val radian = MathUtil.getRadian(mAreaPosition, Point(event.x.toInt(), event.y.toInt()))
                    val angle = getAngleConvert(radian)
                    val distance = MathUtil.getDistance(
                        mAreaPosition.x.toFloat(),
                        mAreaPosition.y.toFloat(),
                        event.x,
                        event.y
                    ).toFloat()
                    listener.callback(EVENT_ACTION, angle, distance)
                }
            }

            if (event.action == MotionEvent.ACTION_UP) {
                mRockerPosition = Point(mAreaPosition)
                mListener?.callback(EVENT_ACTION, -1f, 0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun run() {
        if (isInEditMode) {
            return
        }

        var canvas: Canvas?

        while (mDrawOk) {
            val movable = canMove
            canvas = null
            try {
                if (movable) {
                    canvas = mHolder.lockCanvas()
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    drawArea(canvas)
                    drawRocker(canvas)
                }
                Thread.sleep(mRefreshCycle.toLong())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null && movable) {
                    mHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private fun drawArea(canvas: Canvas) {
        if (mAreaBitmap != null) {
            mPaint.color = Color.BLACK
            val src = Rect(0, 0, mAreaBitmap!!.width, mAreaBitmap!!.height)
            val dst = Rect(
                mAreaPosition.x - mAreaRadius,
                mAreaPosition.y - mAreaRadius,
                mAreaPosition.x + mAreaRadius,
                mAreaPosition.y + mAreaRadius
            )
            canvas.drawBitmap(mAreaBitmap!!, src, dst, mPaint)
        } else {
            mPaint.color = mAreaColor
            canvas.drawCircle(mAreaPosition.x.toFloat(), mAreaPosition.y.toFloat(), mAreaRadius.toFloat(), mPaint)
        }
    }

    private fun drawRocker(canvas: Canvas) {
        if (mRockerBitmap != null) {
            mPaint.color = Color.BLACK
            val src = Rect(0, 0, mRockerBitmap!!.width, mRockerBitmap!!.height)
            val dst = Rect(
                mRockerPosition.x - mRockerRadius,
                mRockerPosition.y - mRockerRadius,
                mRockerPosition.x + mRockerRadius,
                mRockerPosition.y + mRockerRadius
            )
            canvas.drawBitmap(mRockerBitmap!!, src, dst, mPaint)
        } else {
            mPaint.color = mRockerColor
            canvas.drawCircle(mRockerPosition.x.toFloat(), mRockerPosition.y.toFloat(), mRockerRadius.toFloat(), mPaint)
        }
    }

    private fun listenerCallback() {
        mListener?.let { listener ->
            if (mRockerPosition.x == mAreaPosition.x && mRockerPosition.y == mAreaPosition.y) {
                listener.callback(EVENT_CLOCK, -1f, 0f)
            } else {
                val radian = MathUtil.getRadian(mAreaPosition, Point(mRockerPosition.x, mRockerPosition.y))
                val angle = getAngleConvert(radian)
                val distance = MathUtil.getDistance(
                    mAreaPosition.x.toFloat(),
                    mAreaPosition.y.toFloat(),
                    mRockerPosition.x.toFloat(),
                    mRockerPosition.y.toFloat()
                ).toFloat()
                listener.callback(EVENT_CLOCK, angle, distance)
            }
        }
    }

    private fun getAngleConvert(radian: Float): Float {
        return (90 + Math.round(radian / Math.PI.toFloat() * 180)).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            canvas.drawColor(Color.WHITE)
            drawArea(canvas)
            drawRocker(canvas)
        }
    }

    fun setCanMove(isMove: Boolean) {
        canMove = isMove
    }

    fun getAreaRadius(): Int {
        return mAreaRadius
    }

    fun setAreaRadius(areaRadius: Int) {
        mAreaRadius = areaRadius
    }

    fun getRockerRadius(): Int {
        return mRockerRadius
    }

    fun setRockerRadius(rockerRadius: Int) {
        mRockerRadius = rockerRadius
    }

    fun getAreaBitmap(): Bitmap? {
        return mAreaBitmap
    }

    fun setAreaBitmap(areaBitmap: Bitmap?) {
        mAreaBitmap = areaBitmap
    }

    fun getRockerBitmap(): Bitmap? {
        return mRockerBitmap
    }

    fun setRockerBitmap(rockerBitmap: Bitmap?) {
        mRockerBitmap = rockerBitmap
    }

    fun getRefreshCycle(): Int {
        return mRefreshCycle
    }

    fun setRefreshCycle(refreshCycle: Int) {
        mRefreshCycle = refreshCycle
    }

    fun getCallbackCycle(): Int {
        return mCallbackCycle
    }

    fun setCallbackCycle(callbackCycle: Int) {
        mCallbackCycle = callbackCycle
    }

    fun getAreaColor(): Int {
        return mAreaColor
    }

    fun setAreaColor(areaColor: Int) {
        mAreaColor = areaColor
        mAreaBitmap = null
    }

    fun getRockerColor(): Int {
        return mRockerColor
    }

    fun setRockerColor(rockerColor: Int) {
        mRockerColor = rockerColor
        mRockerBitmap = null
    }

    fun setListener(listener: RockerListener) {
        mListener = listener
    }

    interface RockerListener {
        fun callback(eventType: Int, currentAngle: Float, currentDistance: Float)
    }

    companion object {
        private const val DEFAULT_AREA_RADIUS = 100
        private const val DEFAULT_ROCKER_RADIUS = 35

        private val DEFAULT_AREA_COLOR = Color.argb(128, 0, 0, 0)
        private val DEFAULT_ROCKER_COLOR = Color.argb(128, 0, 0, 0)

        private const val DEFAULT_REFRESH_CYCLE = 30
        private const val DEFAULT_CALLBACK_CYCLE = 300

        @JvmField
        val EVENT_ACTION = 1

        @JvmField
        val EVENT_CLOCK = 2

        private var mDrawThread: Thread? = null
        private var mCallbackThread: Thread? = null
        @Volatile
        private var mDrawOk = true
        @Volatile
        private var mCallbackOk = true
    }
}

