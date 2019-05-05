package com.example.cardrive

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

private const val ROTATE_DEFAULT: Long = 1000
private const val MOVE_DEFAULT: Long = 2000
private const val PAINT_PATH_DEFAULT = false

class CarView : View {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val carMatrix = Matrix()
    private var path = Path()

    private val bitmap: Bitmap
    private val isPaintPath: Boolean

    private var tempDegrees = 0f
    private val coordinates: FloatArray = getStartPosition()

    private var isCarMoving = false

    constructor(context: Context, paintPath: Boolean = PAINT_PATH_DEFAULT) : super(context) {
        isPaintPath = paintPath
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.CarView, defStyleAttr, 0)
                .apply {

                    isPaintPath = getBoolean(R.styleable.CarView_paintPath, PAINT_PATH_DEFAULT)

                    recycle()
                }
    }

    init {
        val bm = BitmapFactory.decodeResource(context.resources, R.drawable.car_grey)
        bitmap = bm.getResizedBitmap(newWidth = 100, newHeight = 200)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (!isCarMoving) {
            val rotateAnimator = calculateAngle(ev.x, ev.y)
            val moveAnimator = moveCar(ev.x, ev.y)

            val animator = AnimatorSet()
            animator.playSequentially(rotateAnimator, moveAnimator)
            animator.start()
            true
        } else {
            super.onTouchEvent(ev)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isPaintPath) {
            canvas.drawPath(path, paint)
        }
        carMatrix.setRotate(tempDegrees, bitmap.width / 2f, bitmap.height / 2f)
        carMatrix.postTranslate(coordinates[0] - bitmap.width / 2f, coordinates[1] - bitmap.height / 2f)
        canvas.drawBitmap(bitmap, carMatrix, null)
    }

    private fun getStartPosition(): FloatArray{
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return floatArrayOf((size.x / 2).toFloat(), (size.y - 300).toFloat())
    }

    private fun calculateAngle(destinationX: Float, destinationY: Float): ValueAnimator {
        val startDegrees = tempDegrees
        val endDegrees = getRequiredAngle(coordinates[0], coordinates[1], destinationX, destinationY)

        val rotateAnimator = ValueAnimator.ofFloat(startDegrees, calculateAngleForAnimation(startDegrees, endDegrees))
        rotateAnimator.apply {
            duration = ROTATE_DEFAULT
            addUpdateListener {
                tempDegrees = animatedValue as Float
                invalidate()
            }
            addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            isCarMoving = true
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            tempDegrees = endDegrees
                            Log.d("DEGREE", tempDegrees.toString())
                        }
                    }
            )

        }
        return rotateAnimator
    }

    private fun getRequiredAngle(startX: Float, startY: Float, endX: Float, endY: Float): Float {
        val defY = (startY - endY).toDouble()
        val defX = (startX - endX).toDouble()
        var angle = (Math.toDegrees(Math.atan2(defY, defX))).toFloat()
        angle -= 90 //for vertical car. Without that it will be horizontal
        return angle
    }

    /**
     * Example: startDegrees = 0, endDegrees = 270. Animation will be from 0 to 270 and the car will rotate right.
     * That is not expected for car in real life. It should rotate left. That is why this function recalculate angle
     * for proper animation.
     */
    private fun calculateAngleForAnimation(startDegrees: Float, endDegrees: Float): Float {
        val angleDiff = startDegrees - endDegrees

        if (angleDiff < -180) {
            return endDegrees - 360
        } else if (angleDiff > 180) {
            return endDegrees + 360
        }

        return endDegrees
    }

    private fun moveCar(destinationX: Float, destinationY: Float): ValueAnimator {
        path.reset()
        path.moveTo(coordinates[0], coordinates[1])
        path.lineTo(destinationX, destinationY)
        val pathMeasure = PathMeasure(path, false)

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)

        valueAnimator.apply {
            duration = MOVE_DEFAULT
            addUpdateListener {
                pathMeasure.getPosTan(pathMeasure.length * animatedFraction, coordinates, null)
                invalidate()
            }
            addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            isCarMoving = false
                        }
                    }
            )
        }

        return valueAnimator
    }
}