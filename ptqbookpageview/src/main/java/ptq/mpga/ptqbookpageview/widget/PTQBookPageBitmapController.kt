package ptq.mpga.ptqbookpageview.widget

import android.graphics.Bitmap
import androidx.annotation.IntRange

private const val TAG = "PTQBookPageBitmapController"

private val BITMAP_COLOR_CONFIG = Bitmap.Config.RGB_565

/**
 * 处于当前页时，需要已经绘制好当前页、前一页和后一页的Bitmap，如果在第一页，则不画前一页，在最后同理
 *
 * 使用SideEffect监听重组，如果重组发生，则重画当前三页
 */
internal class PTQBookPageBitmapController(@IntRange(from = 0L) var totalPage: Int) {


    //记录前一页，当前页，后一页，如果current==0，则bitmapBuffer[0]为null。current==total同理
    private val bitmapBuffer = arrayOfNulls<Bitmap?>(3)

    //需要获取的页面，以及对应的BufferIndex
    private var needBitmapPages = mutableListOf<Pair<Int, Int>>()

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
        //这个机制在高速刷新状态时有可能会出错，needBitmapPages也许还没清空，导致needBitmap需求被拒绝，不能正确刷新状态
        //但是为了一般情况下的性能，似乎只能这么做了
        if (needBitmapPages.isNotEmpty()) return
        calculateNeedBitmapPages(page)
        exeRecompositionBlock?.let { it() }
    }

    fun refresh() {
        needBitmapAt(currentPage)
    }

    fun getNeedPage(): Int {
        if (needBitmapPages.isEmpty()) calculateNeedBitmapPages(currentPage)
        return needBitmapPages.first().first
    }

    fun renderThenSave(width: Int, height: Int, render: (usable: Bitmap) -> Unit) {
        //如果不再需要bitmap，则不再绘制了
        if (needBitmapPages.isEmpty() || width == 0 || height == 0) {
            return
        }

        val first = needBitmapPages.first()

        val reuseCandidate = bitmapBuffer[first.second]

        //RGB_565每像素2字节
        if (reuseCandidate == null || width * height * 2 > reuseCandidate.allocationByteCount) {
            reuseCandidate?.recycle()
            bitmapBuffer[first.second] = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        } else {
            bitmapBuffer[first.second]?.let {
                if (width != it.width || height != it.height) {
                    it.reconfigure(width, height, it.config)
                }
            }
        }

        render(bitmapBuffer[first.second]!!)

        needBitmapPages.removeFirst()
        if (needBitmapPages.isEmpty()) return
        exeRecompositionBlock?.let { it() }
    }

//    fun obtainBitmap(width: Int, height: Int): Bitmap? {
//        //如果不再需要bitmap，则不再绘制了
//        if (needBitmapPages.isEmpty()) {
//            return null
//        }
//
//        val first = needBitmapPages.first()
//
//        val reuseCandidate = bitmapBuffer[first.second] ?: return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
//
//        return if (!canReuseBitmap(reuseCandidate, width, height)) {
//            Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
//        } else {
//            reuseCandidate.
//        }
//    }

//    fun saveRenderedBitmap(bitmap: Bitmap) {
//        if (needBitmapPages.isEmpty()) {
//            bitmap.recycle()
//            return
//        }
//
//        val first = needBitmapPages.first()
//
//        needBitmapPages.removeAt(0)
//        bitmapBuffer[first.second]?.recycle()
//        bitmapBuffer[first.second] = bitmap
//
//        if (needBitmapPages.isEmpty()) return
//        exeRecompositionBlock?.let { it() }
//    }

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