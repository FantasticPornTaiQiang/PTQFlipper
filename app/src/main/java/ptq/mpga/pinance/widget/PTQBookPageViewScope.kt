package ptq.mpga.pinance.widget

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

interface PTQBookPageViewScope {
    /**
     * 当用户有想翻页的操作时，会触发此回调。如果处于最后一页，仍想向右翻，则翻页失败，但此回调仍然会调用，可以利用这个回调弹Toast显示没有下一页了
     * @param currentPage 用户操作之后的页面索引，范围是0~pageCount-1
     * @param nextOrPrevious 用户想向前翻页还是向后翻页
     * @param success 用户翻页是否成功，处于最后一页还想向右翻则翻页失败，处于第一页向前翻同理
     */
    fun onPageWantToChange(block: (currentPage: Int, nextOrPrevious: Boolean, success: Boolean) -> Unit)

    /**
     * 页面的内容
     * @param currentPage 表示当前显示的页面索引，范围是0~pageCount-1
     * @param refresh 请在contents的最底部手动调用[refresh]以保证视图正确性
     */
    fun contents(block: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit)
}