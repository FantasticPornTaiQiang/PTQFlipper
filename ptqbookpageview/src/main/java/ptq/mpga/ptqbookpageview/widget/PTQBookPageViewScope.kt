package ptq.mpga.ptqbookpageview.widget

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

interface PTQBookPageViewScope {
    /**
     * 当用户有想翻页的操作时，会触发此回调。如果处于最后一页，仍想向右翻，则翻页失败，但此回调仍然会调用，可以利用这个回调弹Toast显示没有下一页了
     * @param currentPage 用户操作之后的页面索引，范围是0~pageCount-1
     * @param isNextOrPrevious 用户想向前翻页还是向后翻页，true=next
     * @param success 用户翻页是否成功，处于最后一页还想向右翻则翻页失败，处于第一页向前翻同理
     */
    fun onTurnPageRequest(block: (currentPage: Int, isNextOrPrevious: Boolean, success: Boolean) -> Unit)

    /**
     * 页面的内容
     * @param currentPage 表示当前显示的页面索引，范围是0~pageCount-1
     * @param refresh 请在contents的最底部手动调用[refresh]以保证视图正确性
     */
    fun contents(block: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit)

    /**
     * 自定义点击时的翻页行为（会影响页面呈现和[onTurnPageRequest]回调）
     *
     * 默认情况下：
     *
     * （1）点击x>1/2处时翻下一页
     *
     * （2）点击x<1/2处时翻上一页
     *
     * @param leftUP 组件左上角坐标（相对组件的坐标）
     * @param rightDown 组件右下角坐标（相对组件的坐标）
     * @param touchPoint 用户手指触摸坐标（相对组件的坐标）
     * @return 翻页行为，true=下一页，false=上一页，null=不响应
     */
    fun tapBehavior(block: (leftUp: Point, rightDown: Point, touchPoint: Point) -> Boolean?)

    /**
     * 自定义拖动起手的响应条件，这决定了从起手动画开始到能够自由拖动的翻页行为
     *
     * 默认情况下：
     *
     * 从右往左滑则右起手（翻下一页），从左往右划则左起手（翻上一页）
     *
     * @param rightDown 组件右下角坐标（相对组件的坐标）（左上角为0）
     * @param startTouchPoint 用户起手坐标（开始动画前）（相对组件的坐标）
     * @param currentTouchPoint 当前触摸点坐标（相对组件的坐标）
     * @return 翻页行为，true=下一页（从右侧翻开），false=上一页（从左侧翻开），null=不响应
     *
     */
    fun responseDragWhen(block: (rightDown: Point, startTouchPoint: Point, currentTouchPoint: Point) -> Boolean?)

    /**
     * 自定义拖动松手时的翻页行为（会影响页面呈现和[onTurnPageRequest]回调）
     *
     * 默认情况下：
     *
     * （1）起手从右向左，松手在x>1/2处则不翻页
     *
     * （2）起手从右向左，松手在x<1/2处则翻下一页
     *
     * （3）起手从左向右，松手在x>1/2处则翻上一页
     *
     * （4）起手从左向右，松手在x<1/2处则不翻页
     * @param rightDown 组件右下角坐标（相对组件的坐标）（左上角为0）
     * @param initialTouchPoint 用户初始触摸坐标（开始动画前）（相对组件的坐标）
     * @param lastTouchPoint 用户松手坐标（结束动画前）（相对组件的坐标）
     * @param isRightToLeftWhenStart 用户起手是从右向左还是从左向右（即responseDragWhen的结果）
     * @return 翻页行为：第一个参数控制UI动画，true=从左侧退出，false=从右侧退出，第二个参数控制翻页响应，true=下一页，false=上一页，null=不翻页
     */
    fun dragBehavior(block: (rightDown: Point, initialTouchPoint: Point, lastTouchPoint: Point, isRightToLeftWhenStart: Boolean) -> Pair<Boolean, Boolean?>)
}