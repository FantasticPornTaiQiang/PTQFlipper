package ptq.mpga.pinance.widget

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

interface PTQBookPageViewScope {
    /**
     * 当用户有想翻页的操作时，会触发此回调。如果处于最后一页，仍想向右翻，则翻页失败，但此回调仍然会调用，可以利用这个回调弹Toast显示没有下一页了
     * @param currentPage 用户操作之后的页面索引，范围是0~pageCount-1
     * @param nextOrPrevious 用户想向前翻页还是向后翻页，true=next
     * @param success 用户翻页是否成功，处于最后一页还想向右翻则翻页失败，处于第一页向前翻同理
     */
    fun onPageWantToChange(block: (currentPage: Int, isNextOrPrevious: Boolean, success: Boolean) -> Unit)

    /**
     * 页面的内容
     * @param currentPage 表示当前显示的页面索引，范围是0~pageCount-1
     * @param refresh 请在contents的最底部手动调用[refresh]以保证视图正确性
     */
    fun contents(block: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit)

    /**
     * 自定义点击时的翻页行为
     *
     * 默认情况下：
     *
     * （1）点击x>1/2处时翻下一页
     *
     * （2）点击x<1/2处时翻上一页
     * @param leftUp 组件左上角屏幕坐标
     * @param rightDown 组件右下角屏幕坐标
     * @param touchPoint 用户手指触摸屏幕坐标
     * @return 翻页行为，true=下一页，false=上一页，null=不响应
     */
    fun onTapBehavior(block: (leftUp: Offset, rightDown: Offset, touchPoint: Offset) -> Boolean?)

    /**
     * 自定义拖动时的翻页行为
     *
     * 默认情况下：
     *
     * （1）起手在x>1/2处，松手在x>1/2处则不翻页
     *
     * （2）起手在x>1/2处，松手在x<1/2处则翻下一页
     *
     * （3）起手在x<1/2处，松手在x>1/2处则翻上一页
     *
     * （4）起手在x<1/2处，松手在x<1/2处则不翻页
     * @param leftUp 组件左上角屏幕坐标
     * @param rightDown 组件右下角屏幕坐标
     * @param startTouchPoint 用户起手屏幕坐标
     * @param endTouchPoint 用户松手屏幕坐标
     * @param isRightToLeft 手指滑动是否从右向左，也可以通过start和end坐标自行计算
     * @return 翻页行为，true=下一页，false=上一页
     */
    fun onDragBehavior(block: (leftUp: Offset, rightDown: Offset, startTouchPoint: Offset, endTouchPoint: Offset, isRightToLeft: Boolean) -> Boolean)
}