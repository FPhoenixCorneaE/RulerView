package com.fphoenixcorneae.rulerview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.isNotEmpty
import com.fphoenixcorneae.ext.dp2Px
import com.fphoenixcorneae.ext.dpToPx
import com.fphoenixcorneae.util.AudioUtil.mThreadPool
import com.fphoenixcorneae.util.AudioUtil.playAudio
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.abs
import kotlin.math.ceil

/**
 * @desc：刻度尺
 * @date：2021/5/4 15:14
 */
class RulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    /**
     * 刻度尺宽、高
     */
    private var mRulerWidth = 0
    private var mRulerHeight = 0

    /**
     * 速度追踪器、最小速度、最大速度、当前速度
     */
    private var mVelocityTracker: VelocityTracker? = null
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0
    private var mCurrentVelocity = 200

    /**
     * 是否快速滚动
     */
    private var isFastScroll = false

    /**
     * 滚动条
     */
    private var mScroller = Scroller(context)

    /**
     * 触摸事件 X 轴坐标
     */
    private var mLastX = 0f

    /**
     * 刻度尺类型
     */
    private var mRulerType: RulerType = RulerType.HalfHour

    /**
     * 刻度尺回调
     */
    private var mOnRulerCallback: ((selectedValue: Int) -> Unit)? = null

    /**
     * 刻度尺：画笔、可选择的刻度颜色、不可选择的刻度颜色
     */
    private val mRulerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRulerSelectableColor = Color.WHITE
    private val mRulerNonSelectableColor = Color.parseColor("#222222")

    /**
     * 刻度尺值：开始值、结束值、文字大小、顶部外边距、文字字体
     */
    private var mStartValue = 2
    private var mEndValue = 48
    private val mRulerTextSize = 24f.dpToPx()
    private val mRulerTextMarginTop = 20f.dpToPx()
    private val mRulerTextFont = R.font.dinnextltpro_regular

    /**
     * 当前选中的刻度尺值位置、上一个选中的刻度尺值位置
     */
    private var mSelectedRulerValue = 4
    private var mLastSelectedRulerValue = 0

    /**
     * 刻度尺线：高度、细线宽、细线间隔、粗线宽、粗线间隔、不可选择的线数目、两条刻度线之间的间隔
     */
    private val mRulerLineHeight = 40f.dpToPx()
    private val mRulerThinLineWidth = 1f.dpToPx()
    private var mRulerThinLineInterval = 30
    private val mRulerBoldLineWidth = 2f.dpToPx()
    private var mRulerBoldLineInterval = 60
    private val mRulerNonSelectableLineCount = 50
    private var mRulerLinesInterval = 50f.dp2Px()

    // 刻度尺指示器：宽度、颜色（黄色）
    private val mRulerIndicatorLineWidth = 6f.dpToPx()
    private val mRulerIndicatorLineColor = Color.parseColor("#FE4013")

    private var xMap: SparseIntArray? = null

    /**
     * 偏移量：x 轴偏移量、y 轴偏移量、基线偏移量、当前偏移
     */
    private var xOffset = 30f.dpToPx()
    private var yOffset = 0f
    private var mBaselineOffset = 0
    private var mCurrentOffset = 0

    /**
     * 声音数组
     */
    private var mSoundArray: Array<String>? = null
    private var isInit = true
    private var mLastKnobSoundTime: Long = 0

    /**
     * 音频播放阻塞队列
     */
    private var mAudioPlayBlockingQueue: ArrayBlockingQueue<String>? =
        ArrayBlockingQueue<String>(100)

    private var mStartFlingValue = 0

    /**
     * 初始化刻度尺
     * @param rulerType            刻度类型
     * @param startValue           显示的刻度开始值
     * @param endValue             显示的刻度结束值
     * @param defaultSelectedValue 默认选择的刻度值
     * @param onRulerCallback      刻度尺回调
     */
    fun initRuler(
        rulerType: RulerType,
        startValue: Int,
        endValue: Int,
        defaultSelectedValue: Int,
        onRulerCallback: ((selectedValue: Int) -> Unit)? = null
    ) {
        mRulerType = rulerType
        when (rulerType) {
            RulerType.Temperature -> {
                setTemperatureRuler(startValue, endValue, defaultSelectedValue)
            }
            RulerType.HalfHour, RulerType.PerHour -> {
                setHalfOrPerHourTimeRuler(startValue, endValue, defaultSelectedValue)
            }
            RulerType.TenMinute, RulerType.OneMinute -> {
                setTenMinuteTimeRuler(startValue, endValue, defaultSelectedValue)
            }
            RulerType.Probe -> {
                setProveTimeRuler(startValue, endValue, defaultSelectedValue)
            }
            RulerType.Pieces -> {
                initAudioEnum()
                setPiecesRuler(startValue, endValue, defaultSelectedValue)
            }
        }
        mOnRulerCallback = onRulerCallback
    }

    private fun initAudioEnum() {
        mSoundArray = arrayOf(
            "slider_tick_1.wav",
            "slider_tick_2.wav",
            "slider_tick_3.wav",
            "slider_tick_4.wav",
            "slider_tick_5.wav",
            "slider_tick_6.wav",
            "slider_tick_7.wav",
            "slider_tick_8.wav",
            "slider_tick_9.wav",
            "slider_tick_10.wav",
            "slider_tick_11.wav",
            "slider_tick_12.wav",
            "slider_tick_13.wav",
            "slider_tick_14.wav",
            "slider_tick_15.wav",
            "slider_tick_16.wav",
            "slider_tick_17.wav"
        )
    }

    /**
     * 片数样式刻度尺
     *
     * @param startValue
     * @param endValue
     * @param defaultSelectedValue 默认选择值
     */
    private fun setPiecesRuler(startValue: Int, endValue: Int, defaultSelectedValue: Int) {
        mCurrentVelocity = 100
        mRulerLinesInterval = 50f.dp2Px()
        xOffset = 30f.dpToPx()
        mRulerBoldLineInterval = 1
        mRulerThinLineInterval = 1
        mStartValue = startValue / mRulerThinLineInterval
        mEndValue = endValue / mRulerThinLineInterval
        mSelectedRulerValue = defaultSelectedValue / mRulerThinLineInterval
    }

    /**
     * 温度样式刻度尺
     *
     * @param startValue
     * @param endValue
     * @param defaultSelectedValue 默认选择值
     */
    private fun setTemperatureRuler(startValue: Int, endValue: Int, defaultSelectedValue: Int) {
        mCurrentVelocity = 200
        mRulerLinesInterval = 19f.dp2Px()
        xOffset = 10f.dpToPx()
        mRulerBoldLineInterval = 20
        mRulerThinLineInterval = 5
        mStartValue = startValue / mRulerThinLineInterval
        mEndValue = endValue / mRulerThinLineInterval
        mSelectedRulerValue = defaultSelectedValue / mRulerThinLineInterval
    }

    /**
     * 时间样式刻度尺,每半小时一个刻度
     *
     * @param startValue
     * @param endValue
     * @param defaultSelectedValue 默认选择值
     */
    private fun setHalfOrPerHourTimeRuler(
        startValue: Int,
        endValue: Int,
        defaultSelectedValue: Int
    ) {
        mCurrentVelocity = 200
        mRulerLinesInterval = 50f.dp2Px()
        xOffset = 30f.dpToPx()
        mRulerBoldLineInterval = if (mRulerType == RulerType.PerHour) 120 else 60
        mRulerThinLineInterval = if (mRulerType == RulerType.PerHour) 60 else 30
        mStartValue = startValue / mRulerThinLineInterval
        mEndValue = endValue / mRulerThinLineInterval
        mSelectedRulerValue = defaultSelectedValue / mRulerThinLineInterval
    }

    /**
     * 探针样式刻度尺,每1度一个刻度
     *
     * @param startValue
     * @param endValue
     * @param defaultSelectedValue 默认选择值
     */
    private fun setProveTimeRuler(startValue: Int, endValue: Int, defaultSelectedValue: Int) {
        mCurrentVelocity = 200
        mRulerLinesInterval = 19f.dp2Px()
        xOffset = 10f.dpToPx()
        mRulerBoldLineInterval = 4
        mRulerThinLineInterval = 1
        mStartValue = startValue / mRulerThinLineInterval
        mEndValue = endValue / mRulerThinLineInterval
        mSelectedRulerValue = defaultSelectedValue / mRulerThinLineInterval
    }

    /**
     * 时间样式刻度尺,每10分钟一个刻度
     *
     * @param startValue
     * @param endValue
     * @param defaultSelectedValue 默认选择值
     */
    private fun setTenMinuteTimeRuler(startValue: Int, endValue: Int, defaultSelectedValue: Int) {
        mCurrentVelocity = 200
        mRulerLinesInterval = 10f.dp2Px()
        mRulerBoldLineInterval = 10
        mRulerThinLineInterval = 1
        mStartValue = startValue / mRulerThinLineInterval
        mEndValue = endValue / mRulerThinLineInterval
        mSelectedRulerValue = defaultSelectedValue / mRulerThinLineInterval
    }

    private fun startPlayThread() {
        mThreadPool.execute {
            while (isAttachedToWindow) {
                try {
                    playAudio(mAudioPlayBlockingQueue!!.take())
                    SystemClock.sleep(70)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            mAudioPlayBlockingQueue?.clear()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mAudioPlayBlockingQueue?.clear()
        mAudioPlayBlockingQueue = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRulerWidth = w
        mRulerHeight = h
        yOffset = h / 2f - (mRulerLineHeight + mRulerTextMarginTop + mRulerTextSize) / 2
        mBaselineOffset = w / 2
        if (null == xMap) {
            xMap = SparseIntArray()
            for (i in mStartValue..mEndValue) {
                var x =
                    (i - mStartValue) * mRulerLinesInterval - mBaselineOffset + mBaselineOffset % mRulerLinesInterval
                if (x % mRulerLinesInterval != 0) {
                    x -= x % mRulerLinesInterval
                }
                xMap!!.put(i, x)
            }
        }
        if (null != xMap && xMap!!.isNotEmpty()) {
            scrollTo(xMap!![mSelectedRulerValue], 0)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in mStartValue - mRulerNonSelectableLineCount until mEndValue + 1 + mRulerNonSelectableLineCount) {
            val startX = (i - mStartValue) * mRulerLinesInterval
            if (i * mRulerThinLineInterval % mRulerBoldLineInterval == 0
                || ((mRulerType == RulerType.TenMinute || mRulerType == RulerType.OneMinute) && i == mStartValue)
            ) {
                if (startX > 0 || startX < mRulerWidth) {
                    // 画文字
                    if (i in mStartValue..mEndValue) {
                        drawRulerText(canvas, i, startX + xOffset)
                    }
                }
            }
            if (startX > 0 || startX < mRulerWidth) {
                // 画尺
                var isBold = i * mRulerThinLineInterval % mRulerBoldLineInterval == 0
                        || (i == mStartValue && (mRulerType == RulerType.TenMinute || mRulerType == RulerType.OneMinute))
                if (i * mRulerThinLineInterval == 0 && mRulerType == RulerType.TenMinute) {
                    isBold = false
                }
                drawRulerLine(canvas, i in mStartValue..mEndValue, isBold, startX + xOffset)
            }
        }
        // 刻度指示器线
        drawRulerIndicatorLine(canvas)
    }

    /**
     * 绘制刻度尺值
     */
    private fun drawRulerText(canvas: Canvas, i: Int, x: Float) {
        mRulerPaint.apply {
            if (!isInEditMode) {
                typeface = ResourcesCompat.getFont(context, mRulerTextFont)
            }
            color = mRulerSelectableColor
            textSize = mRulerTextSize
        }
        when (mRulerType) {
            RulerType.HalfHour, RulerType.PerHour -> {
                val text = (i * mRulerThinLineInterval / 60).toString() + ":00"
                val textWidth = mRulerPaint.getTextWidth(text)
                canvas.drawText(
                    text,
                    x - textWidth / 2,
                    yOffset + mRulerTextMarginTop + mRulerLineHeight + mRulerTextSize,
                    mRulerPaint
                )
            }
            RulerType.Temperature, RulerType.TenMinute, RulerType.Pieces, RulerType.Probe, RulerType.OneMinute -> {
                val text = (i * mRulerThinLineInterval).toString()
                val textWidth = mRulerPaint.getTextWidth(text)
                canvas.drawText(
                    text,
                    x - textWidth / 2,
                    yOffset + mRulerTextMarginTop + mRulerLineHeight + mRulerTextSize,
                    mRulerPaint
                )
            }
        }
    }

    /**
     * 绘制刻度尺线
     *
     * @param canvas     画布
     * @param selectable 是否可选择的
     * @param boldLine   是否粗刻度线
     * @param startX     X轴开始坐标
     */
    private fun drawRulerLine(
        canvas: Canvas,
        selectable: Boolean,
        boldLine: Boolean,
        startX: Float
    ) {
        mRulerPaint.apply {
            color = if (selectable) mRulerSelectableColor else mRulerNonSelectableColor
            strokeWidth = if (selectable && boldLine) mRulerBoldLineWidth else mRulerThinLineWidth
        }
        canvas.drawLine(
            startX,
            yOffset,
            startX,
            yOffset + mRulerLineHeight,
            mRulerPaint
        )
    }

    /**
     * 绘制刻度指示器线
     */
    private fun drawRulerIndicatorLine(canvas: Canvas) {
        mRulerPaint.apply {
            color = mRulerIndicatorLineColor
            strokeWidth = mRulerIndicatorLineWidth
            style = Paint.Style.FILL
        }
        // 指示器高度，比刻度线高度多8dp
        val extraSelectedLine = 8.dpToPx() / 2
        val startX = mBaselineOffset + scrollX - mBaselineOffset % mRulerLinesInterval
        canvas.drawLine(
            startX + xOffset,
            yOffset - extraSelectedLine,
            startX + xOffset,
            yOffset + mRulerLineHeight + extraSelectedLine,
            mRulerPaint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        obtainVelocityTracker()
        mVelocityTracker!!.addMovement(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mScroller.forceFinished(true)
                mLastX = event.x
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                mStartFlingValue = getCurrentValue(scrollX)
            }
            MotionEvent.ACTION_MOVE -> {
                isFastScroll = false
                val moveX = event.x
                mCurrentOffset = (moveX - mLastX).toInt()
                val scrollXDistance = (scrollX - mCurrentOffset * 0.45).toInt()
                if (scrollXDistance >= xMap!![mStartValue] && scrollXDistance <= xMap!![mEndValue]) {
                    scrollTo(scrollXDistance, 0)
                    mLastX = moveX
                } else {
                    mScroller.forceFinished(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 避免快速来回滑动加入过多音频
                while (mAudioPlayBlockingQueue!!.size > 5) {
                    mAudioPlayBlockingQueue!!.remove()
                }
                mVelocityTracker!!.computeCurrentVelocity(
                    mCurrentVelocity,
                    mMaximumVelocity.toFloat()
                )
                val initialVelocity = mVelocityTracker!!.xVelocity.toInt()
                if (abs(initialVelocity) > mMinimumVelocity) {
                    isFastScroll = true
                    flingX((-(initialVelocity / 1.7f)).toInt())
                } else {
                    try {
                        mScroller.startScroll(
                            scrollX,
                            0,
                            xMap!![mSelectedRulerValue] - scrollX,
                            0,
                            SCROLL_DURATION
                        )
                        invalidate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                releaseVelocityTracker()
            }
        }
        return true
    }

    /**
     * 惯性滑动
     *
     * @param velocityX
     */
    private fun flingX(velocityX: Int) {
        mScroller.fling(
            scrollX,
            scrollY,
            velocityX,
            0,
            xMap!![mStartValue],
            xMap!![mEndValue],
            0,
            0
        )
        awakenScrollBars(mScroller.duration)
        invalidate()
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (isFastScroll) {
                isFastScroll = false
                var targetValue = getCurrentValue(mScroller.finalX)
                if (abs(targetValue - mStartFlingValue) > MAX_FLING_VALUE) {
                    targetValue = if (mCurrentOffset > 0) {
                        mStartFlingValue - MAX_FLING_VALUE
                    } else {
                        mStartFlingValue + MAX_FLING_VALUE
                    }
                }
                if (targetValue >= mEndValue) {
                    targetValue = mEndValue
                } else if (targetValue <= mStartValue) {
                    targetValue = mStartValue
                }
                val integer = xMap!![targetValue]
                mScroller.startScroll(scrollX, 0, integer - scrollX, 0, SCROLL_DURATION)
                postInvalidate()
            } else {
                val x = mScroller.currX
                scrollTo(x, 0)
                postInvalidate()
            }
        }
    }

    /**
     * 计算并回调位置信息
     *
     * @param scrollX
     */
    private fun computeAndCallback(scrollX: Int) {
        mSelectedRulerValue = getCurrentValue(scrollX)
        mSelectedRulerValue = abs(mSelectedRulerValue)
        if (mSelectedRulerValue > mEndValue) {
            mSelectedRulerValue = mEndValue
            scrollTo(xMap!![mEndValue], 0)
        } else if (mSelectedRulerValue < mStartValue) {
            mSelectedRulerValue = mStartValue
            scrollTo(xMap!![mStartValue], 0)
        }
        // 选中回调
        mOnRulerCallback?.invoke(mSelectedRulerValue * mRulerThinLineInterval)

        if (mLastSelectedRulerValue != mSelectedRulerValue) {
            playSounds(mSelectedRulerValue)
            mLastSelectedRulerValue = mSelectedRulerValue
        }
    }

    private fun playSounds(number: Int) {
        if (!isInit) {
            if (mRulerType == RulerType.Pieces) {
                mSoundArray?.let {
                    if (7 + number < it.size) {
                        playAudio(it[7 + number])
                    }
                }
            } else {
                if (number * mRulerThinLineInterval % mRulerBoldLineInterval == 0
                    && mRulerType == RulerType.Temperature
                ) {
                    when (number * mRulerThinLineInterval) {
                        40 -> mAudioPlayBlockingQueue!!.add("slider_tick_6.wav")
                        60 -> mAudioPlayBlockingQueue!!.add("slider_tick_7.wav")
                        80 -> mAudioPlayBlockingQueue!!.add("slider_tick_8.wav")
                        100 -> mAudioPlayBlockingQueue!!.add("slider_tick_9.wav")
                        120 -> mAudioPlayBlockingQueue!!.add("slider_tick_10.wav")
                        140 -> mAudioPlayBlockingQueue!!.add("slider_tick_11.wav")
                        160 -> mAudioPlayBlockingQueue!!.add("slider_tick_12.wav")
                        180 -> mAudioPlayBlockingQueue!!.add("slider_tick_13.wav")
                        200 -> mAudioPlayBlockingQueue!!.add("slider_tick_14.wav")
                        220 -> mAudioPlayBlockingQueue!!.add("slider_tick_15.wav")
                        240 -> mAudioPlayBlockingQueue!!.add("slider_tick_16.wav")
                        260 -> mAudioPlayBlockingQueue!!.add("slider_tick_17.wav")
                    }
                } else {
                    val currentTimeMillis = System.currentTimeMillis()
                    if (currentTimeMillis - mLastKnobSoundTime >= 50) {
                        playAudio("knob_turn.wav")
                        mLastKnobSoundTime = currentTimeMillis
                    }
                }
            }
        }
        if (isInit) {
            isInit = false
        }
    }

    private fun getCurrentValue(scrollX: Int): Int {
        var finalX = mBaselineOffset + scrollX
        if (finalX % mRulerLinesInterval != 0) {
            finalX -= finalX % mRulerLinesInterval
        }
        return mStartValue + finalX / mRulerLinesInterval
    }

    /**
     * 精确计算文字宽度
     */
    private fun Paint.getTextWidth(text: String?): Float {
        var iRet = 0f
        if (text != null && text.isNotEmpty()) {
            val len = text.length
            val widths = FloatArray(len)
            getTextWidths(text, widths)
            for (j in 0 until len) {
                iRet += ceil(widths[j].toDouble()).toFloat()
            }
        }
        return iRet
    }

    /**
     * 初始化速度追踪器
     */
    private fun obtainVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    /**
     * 释放速度追踪器
     */
    private fun releaseVelocityTracker() {
        mVelocityTracker?.recycle()
        mVelocityTracker = null
    }

    /**
     * 刻度尺类型
     */
    enum class RulerType {
        Temperature, HalfHour, TenMinute, Probe, Pieces,

        /**
         * HalfHour样式一致
         */
        PerHour,

        /**
         * Pieces样式一致
         */
        OneMinute
    }

    init {
        val viewConfiguration = ViewConfiguration.get(context)
        mMinimumVelocity = viewConfiguration.scaledMinimumFlingVelocity
        mMaximumVelocity = viewConfiguration.scaledMaximumFlingVelocity
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            setOnScrollChangeListener { _: View?, scrollX: Int, _: Int, oldScrollX: Int, _: Int ->
                if (isInit) {
                    startPlayThread()
                    computeAndCallback(scrollX)
                    isInit = false
                } else {
                    var i = oldScrollX
                    if (oldScrollX < scrollX) {
                        while (i != scrollX) {
                            if (i + mRulerLinesInterval >= scrollX) {
                                i = scrollX
                            } else {
                                i += mRulerLinesInterval
                            }
                            computeAndCallback(i)
                        }
                    } else {
                        while (i != scrollX) {
                            if (i - mRulerLinesInterval <= scrollX) {
                                i = scrollX
                            } else {
                                i -= mRulerLinesInterval
                            }
                            computeAndCallback(i)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val MAX_FLING_VALUE = 34

        /**
         * 滑动到刻度尺的动画时间
         */
        private const val SCROLL_DURATION = 500
    }
}