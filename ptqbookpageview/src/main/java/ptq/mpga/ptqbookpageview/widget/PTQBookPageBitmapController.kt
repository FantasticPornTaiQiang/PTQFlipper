package ptq.mpga.ptqbookpageview.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.annotation.IntRange

private const val TAG = "PTQBookPageBitmapController"

private val BITMAP_COLOR_CONFIG = Bitmap.Config.ARGB_8888

/**
 * 处于当前页时，需要已经绘制好当前页、前一页和后一页的Bitmap，如果在第一页，则不画前一页，在最后同理
 *
 * 使用SideEffect监听重组，如果重组发生，则重画当前三页
 */
internal class PTQBookPageBitmapController(@IntRange(from = 0L) var totalPage: Int) {

    //记录前一页，当前页，后一页，如果current==0，则bitmapBuffer[0]为null。current==total同理
    private val bitmapBuffer = arrayOfNulls<Bitmap?>(3)

//    val bitmapSynthesizer = BitmapSynthesizer()

    //需要获取的页面，以及对应的BufferIndex
    private var needBitmapPages = mutableListOf<Pair<Int, Int>>()

    private val canvas = Canvas()

    //当前页面
    var currentPage = 0
        private set

    var exeRecompositionBlock: (() -> Unit)? = null
        set(value) {
            if (field != null) return
            field = value
        }

    private fun calculateNeedBitmapPages(page: Int) {
        if (page !in 0 until totalPage) return

        currentPage = page

        //需要缓存的页数范围
        val needs = mutableListOf<Pair<Int, Int>>()
        if (page > 0) {
            needs.add(Pair(page - 1, 0))
        }
        needs.add(Pair(page, 1))
        if (page < totalPage - 1) {
            needs.add(Pair(page + 1, 2))
        }

        needBitmapPages = needs
    }

    fun needBitmapAt(page: Int) {
        //这个机制在高速刷新状态时（例如LazyColumn）有可能会出错，needBitmapPages也许还没清空，导致needBitmap需求被拒绝，不能正确刷新状态
        //但是为了一般情况下的性能，似乎只能这么做了
        if (needBitmapPages.isNotEmpty()) return
        calculateNeedBitmapPages(page)
        exeRecompositionBlock?.let { it() }
//        bitmapSynthesizer.clearSynthesizedCache()
    }

    fun refresh() {
        needBitmapAt(currentPage)
    }

    fun getNeedPage(): Int {
        if (needBitmapPages.isEmpty()) calculateNeedBitmapPages(currentPage)
        return needBitmapPages.first().first
    }

    /**
     * 只分配一次内存，除非新的size有变化
     * @since v1.1.0
     */
    fun renderThenSave(width: Int, height: Int, render: (drawable: Canvas) -> Unit) {
        //如果不再需要bitmap，则不再绘制了
        if (needBitmapPages.isEmpty() || width <= 0 || height <= 0) {
            return
        }

        val first = needBitmapPages.first()

        var needNew = false
        if (bitmapBuffer[first.second] == null) {
            needNew = true
        } else {
            //新的大小发生变化（因为config不变，所以bitmap的大小可以认为只受width, height影响，而不再去计算allocationByteCount）
            bitmapBuffer[first.second]!!.let {
                if (width != it.width || height != it.height) {
                    it.recycle()
                    needNew = true
                }
            }
        }

        if (needNew) {
            bitmapBuffer[first.second] = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

//            with(bitmapSynthesizer) {
//                if (synthesizedBitmap.width != width || synthesizedBitmap.height != height) {
//                    synthesizedBitmap.recycle()
//                    synthesizedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//                    resize(width, height)
//                }
//            }
        }

        canvas.let {
            it.setBitmap(bitmapBuffer[first.second]!!)
            render(it)
            it.setBitmap(null)
        }

        needBitmapPages.removeFirst()
        if (needBitmapPages.isEmpty()) return
        exeRecompositionBlock?.let { it() }
    }

    fun destroy() {
        bitmapBuffer.forEach {
            it?.recycle()
        }
//        bitmapSynthesizer.destroy()
    }

    /**
     * 获取当前Bitmap
     * @param which 0=前一张 1=当前 2=下一张
     */
    fun getBitmapCurrent(which: Int): Bitmap {
        if (bitmapBuffer[which] == null) {
            bitmapBuffer[which] = Bitmap.createBitmap(1, 1, BITMAP_COLOR_CONFIG)
        }
        return bitmapBuffer[which]!!
    }

    fun isRenderOk() = needBitmapPages.isEmpty()
}

///**
// * Bitmap合成器，在native层合成lowerBitmap和distortedBitmap
// * 把BitmapSynthesizer放进BitmapController里是为了生命周期方便一并管理
// * @since v1.1.0
// */
//internal class BitmapSynthesizer {
//    var synthesizedBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
//
//    private external fun nSynthesize(
//        lower: Bitmap,
//        synthesized: Bitmap,
//        WZS: FloatArray,
//        upsideDown: Boolean,
//    )
//
//    private external fun nResize(width: Int, height: Int, synthesized: Bitmap)
//    private external fun nDestroy()
//    private external fun nClearSynthesizedCache(synthesized: Bitmap)
//
//    companion object {
//        init {
//            System.loadLibrary("bitmap-mesh")
//        }
//    }
//
//    fun destroy() {
//        synthesizedBitmap.recycle()
//        nDestroy()
//    }
//
//    fun resize(width: Int, height: Int) = nResize(width, height, synthesizedBitmap)
//
//    /**
//     * 合成distortion和background
//     * @since v1.1.0
//     */
//    fun synthesize(
//        lower: Bitmap,
//        W: Point,
//        Z: Point,
//        S: Point,
//        upsideDown: Boolean,
//    ) {
//        nSynthesize(
//            lower,
//            synthesizedBitmap,
//            floatArrayOf(W.x, W.y, Z.x, Z.y, S.x, S.y),
//            upsideDown
//        )
//    }
//
//    fun clearSynthesizedCache() {
//        nClearSynthesizedCache(synthesizedBitmap)
//    }
//}