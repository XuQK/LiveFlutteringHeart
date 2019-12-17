package github.xuqk.liveflutteringheart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Created By：XuQK
 * Created Date：2019-12-17 12:57
 * Creator Email：xuqiankun66@gmail.com
 * Description：
 */

class FlutteringHeartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), LifecycleObserver {

    // 1/4个周期的尺寸
    private val waveLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
    private val waveRange = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)

    private var imgSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    private val path: Path = Path()
    private val paint: Paint = Paint()
    private var waveLoop: Int = 0
    private val random = Random(1000)
    private var duration: Float = 0f
    private var speed: Float = 1f

    private val heartSet = ConcurrentLinkedDeque<LiveHeart>()
    private val heartImageSet = mutableListOf<Bitmap>()

    init {
        (context as? LifecycleOwner)?.lifecycle?.addObserver(this)
    }

    /**
     * @param imageSize 心心图片的尺寸，要求正方形，单位dp
     * @param duration 单个心心飘动的时间，毫秒
     * @param anchorDrawableRes 初始心心
     * @param heartDrawableRes 飘动的心心图片集
     */
    fun init(imageSize: Float, duration: Float, speed: Float, @DrawableRes anchorDrawableRes: Int, @DrawableRes vararg heartDrawableRes: Int) {
        imgSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageSize, resources.displayMetrics)
        this.duration = duration
        this.speed = speed

        heartDrawableRes.forEach {
            val b = BitmapFactory.decodeResource(context.resources, it)
            heartImageSet.add(Bitmap.createScaledBitmap(b, imgSize.toInt(), imgSize.toInt(), true))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 计算path周期数
        waveLoop = ceil(((sqrt((4 * waveLength * waveLength) + 8 * waveLength * MeasureSpec.getSize(heightMeasureSpec)) - 2 * waveLength) / 4 / waveLength)).toInt()
        // 指定view宽度为图片宽度的5倍
        super.onMeasure(MeasureSpec.makeMeasureSpec((imgSize * 5).toInt(), MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        heartSet.forEach {
            if (it.alpha > 0) {
                paint.reset()
                paint.alpha = it.alpha
                canvas.drawBitmap(heartImageSet[it.bitmapIndex], it.x, it.y, paint)
            }
        }
    }

    /**
     * 创建一个heart对象
     */
    private fun makeHeart(): LiveHeart {
        // 贝塞尔曲线初始方向
        val initialDirection = if (Math.random() < 0.5) -1 else 1

        // 曲线的变化范围
        val fraction = Math.random().toFloat() * 4
        val maxFraction = width.toFloat() / 2 / waveLength
        val scaleFraction = when {
            fraction < 0.5 -> 0.5f
            fraction > maxFraction -> maxFraction
            else -> fraction
        }

        // 这里建立的path轨迹，是图片左上角，即绘制起点(0,0)的轨迹
        // 该轨迹为二阶贝塞尔曲线，每一个周期的长度都增加一倍
        path.reset()
        path.moveTo((width.toFloat() / 2) - (imgSize / 2), height.toFloat() - imgSize)
        for (i in 1..waveLoop) {
            val j = i * initialDirection
            path.rQuadTo(-waveRange * scaleFraction * j, -waveLength * i, 0f, -2 * waveLength * i)
            path.rQuadTo(waveRange * scaleFraction * j, -waveLength * i, 0f, -2 * waveLength * i)
        }

        return LiveHeart(height.toFloat(), duration, speed,path,random.nextInt(heartImageSet.size))
    }

    /**
     * 添加一个心，并开始飘动
     */
    fun shoot() {
        heartSet.add(makeHeart())

        if (!running) {
            startRun()
        }
    }

    private var running = false
    private var pause = false
    private fun startRun() {
        // 如果是暂停状态，停止绘制
        if (pause) return

        post {
            running = true
            // 先计算现存的每一个心的位置，并移除已完成的心

            heartSet.forEach {
                it.calculateCurrentPoint()
                if (it.x == -1f && it.y == -1f) {
                    heartSet.remove(it)
                }
            }

            // 计算完毕后，如果还有需要飘动的心，开始下一轮
            if (heartSet.isNotEmpty()) {
                invalidate()
                postDelayed({ startRun() }, 32)
            } else {
                running = false
                invalidate()
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(owner: LifecycleOwner) {
        heartSet.clear()
        invalidate()
        pause = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume(owner: LifecycleOwner) {
        pause = false
    }
}

/**
 * 发射一个心就要新建一个对应的对象
 */
class LiveHeart(
    maxHeight: Float = 0f,
    private val duration: Float = 4000f,
    speed: Float = 1f,
    path: Path = Path(),
    val bitmapIndex: Int = 0
) {

    private val pathMeasure: PathMeasure = PathMeasure(path, false)
    private val point: FloatArray = floatArrayOf(0f, 0f)
    private val tanPoint: FloatArray = floatArrayOf(0f, 0f)

    private var fraction: Float = 0f
    private val fractionStep: Float = 16f * speed
    var alpha: Int = 255
    /** 设定离最大高度还有1/3时开始透明，离最大高度还有20像素时，完全透明 */
    private val alphaLength = maxHeight / 3 - 20

    val x: Float
        get() = point[0]
    val y: Float
        get() = point[1]

    /**
     * 这里计算在给定的fraction下heart的坐标，坐标超出范围即设为-1
     */
    fun calculateCurrentPoint() {
        fraction += (fractionStep / duration)
        if (fraction > 1) {
            point[0] = -1f
            point[1] = -1f
            return
        }

        pathMeasure.getPosTan(pathMeasure.length * fraction, point, tanPoint)

        if (y < 0) {
            point[0] = -1f
            point[1] = -1f
        } else {
            // 计算透明度，离最大高度还有1/3时开始透明，离最大高度还有20像素时，完全透明
            alpha = when {
                y < 20 -> 0
                y > alphaLength + 20 -> 255
                else -> (y / alphaLength * 255).toInt()
            }
            if (alpha > 255) alpha = 255
        }
    }
}
