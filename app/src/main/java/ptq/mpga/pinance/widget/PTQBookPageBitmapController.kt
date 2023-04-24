package ptq.mpga.pinance.widget

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import kotlin.math.log

private const val TAG = "PTQBookPageBitmapController"

/**
 * 处于当前页时，需要已经绘制好当前页、前一页和后一页的Bitmap，如果在第一页，则不画前一页，在最后同理
 *
 * 使用SideEffect监听重组，如果重组发生，则重画当前三页
 */
internal class PTQBookPageBitmapController(@IntRange(from = 1) val totalPage: Int) {

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

//    init {
//        calculateNeedBitmapPages(0)
//    }

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

    fun saveRenderedBitmap(bitmap: Bitmap) {
        if (needBitmapPages.isEmpty()) {
            bitmap.recycle()
            return
        }

        val first = needBitmapPages.first()
        needBitmapPages.removeAt(0)
        bitmapBuffer[first.second]?.recycle()
        bitmapBuffer[first.second] = bitmap

        if (needBitmapPages.isEmpty()) return
        exeRecompositionBlock?.let { it() }
    }

    /**
     * @param 0=前一张 1=当前 2=下一张
     */
    fun getBitmapCurrent(which: Int): Bitmap {
        if (bitmapBuffer[which] == null) {
            bitmapBuffer[which] = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        }
        return bitmapBuffer[which]!!
    }

    fun isRenderOk() = needBitmapPages.isEmpty()

}