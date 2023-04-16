package ptq.mpga.pinance.widget

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import kotlin.math.log

private const val TAG = "PTQBookPageBitmapController"

internal class PTQBookPageBitmapController(@IntRange(from = 1) val totalPage: Int) {

    //记录前一页，当前页，后一页，如果current==0，则bitmapBuffer[0]为null。current==total同理
    private val bitmapBuffer = arrayOfNulls<Bitmap?>(3)

    //需要获取的页面，以及对应的BufferIndex
    private var needBitmapPages = mutableListOf<Pair<Int, Int>>()

    //当前页面
    var currentPage = 0

    var exeRecompositionBlock: (() -> Unit)? = null
    set(value) {
        if (field != null) return
        field = value
    }

    init {
        calculateNeedBitmapPages(0)
    }

    fun refreshCurrent(bitmap: Bitmap) {
        bitmapBuffer[1]?.recycle()
        bitmapBuffer[1] = bitmap
    }

    private fun calculateNeedBitmapPages(page: Int) {
        if (page !in 0 until totalPage) return

        currentPage = page

        //需要缓存的页数范围
        val pageRangeList = ((page - 1).coerceAtLeast(0)..(page + 1).coerceAtMost(totalPage - 1)).toList()

        //对应的BufferList中的索引
        val bufferOffset = if (page == 0) 1 else 0

        val needBufferIndexes = (bufferOffset until (bufferOffset + pageRangeList.size)).toList()

        needBitmapPages = pageRangeList.mapIndexed { index, i ->
            Pair(i, needBufferIndexes[index])
        }.toMutableList()
    }

    fun needBitmapAt(page: Int) {
        calculateNeedBitmapPages(page)
        exeRecompositionBlock?.let { it() }
    }

    fun getNeedPage() = if (needBitmapPages.isEmpty()) currentPage else needBitmapPages.first().first

    fun saveRenderedBitmap(bitmap: Bitmap) {
        if (needBitmapPages.isEmpty()) return
        val first = needBitmapPages.first()
        needBitmapPages.removeAt(0)
        bitmapBuffer[first.second]?.recycle()
        bitmapBuffer[first.second] = bitmap

        if (needBitmapPages.isEmpty()) return
        exeRecompositionBlock?.let { it() }
    }

    fun getCurrentBitmaps() = bitmapBuffer.map { it ?: Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565) }
}