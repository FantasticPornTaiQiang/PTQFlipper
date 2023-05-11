package ptq.mpga.pinance.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "PTQBookPageView"

data class PTQBookPageViewConfig(
    val pageColor: Color,
    val disabled: Boolean
)

val LocalPTQBookPageViewConfig = compositionLocalOf<PTQBookPageViewConfig> { error("Local flipper config error") }

internal class PTQBookPageViewScopeImpl : PTQBookPageViewScope {
    var contents: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit = { _, _ -> }
    var onPageWantToChange: (Int, Boolean, Boolean) -> Unit = { _, _, _ -> }
    var tapBehavior: ((leftUp: Point, rightDown: Point, touchPoint: Point) -> Boolean?)? = null
    var dragBehavior: ((leftUp: Point, rightDown: Point, initialTouchPoint: Point, lastTouchPoint: Point, isRightToLeftWhenStart: Boolean) -> Pair<Boolean, Boolean?>)? = null
    var responseDragWhen: ((leftUp: Point, rightDown: Point, startTouchPoint: Point, currentTouchPoint: Point) -> Boolean?)? = null

    override fun onUserWantToChange(block: (currentPage: Int, nextOrPrevious: Boolean, success: Boolean) -> Unit) {
        onPageWantToChange = block
    }

    override fun contents(block: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit) {
        contents = block
    }

    override fun tapBehavior(block: (leftUp: Point, rightDown: Point, touchPoint: Point) -> Boolean?) {
        tapBehavior = block
    }

    override fun responseDragWhen(block: (leftUp: Point, rightDown: Point, startTouchPoint: Point, currentTouchPoint: Point) -> Boolean?) {
        responseDragWhen = block
    }

    override fun dragBehavior(block: (leftUp: Point, rightDown: Point, initialTouchPoint: Point, lastTouchPoint: Point, isRightToLeftWhenStart: Boolean) -> Pair<Boolean, Boolean?>) {
        dragBehavior = block
    }
}

/**
 * @param modifier 修饰符
 * @param pageColor 背页页面颜色
 * @param state 设置页面总数和当前页数，如果页面总数小于当前页数，则会引发异常，具体参见[PTQBookPageViewState]以及[rememberPTQBookPageViewState]
 * @param ptqBookPageViewScope 翻页器提供的各类回调 [PTQBookPageViewScope]
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PTQBookPageView(
    modifier: Modifier = Modifier, pageColor: Color = Color.White, state: PTQBookPageViewState, ptqBookPageViewScope: PTQBookPageViewScope.() -> Unit
) {
    Box(
        modifier.fillMaxSize()
    ) {
        var size by remember { mutableStateOf(IntSize.Zero) }

        val controller by remember {
            mutableStateOf(PTQBookPageBitmapController(state.pageCount))
        }

        remember(state) {
            controller.totalPage = state.pageCount
            state.currentPage?.let {
                controller.needBitmapAt(it)
            }
            mutableStateOf(state)
        }

        val callbacks = rememberUpdatedState(PTQBookPageViewScopeImpl().apply(ptqBookPageViewScope))

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                object : AbstractComposeView(context) {

                    @Composable
                    override fun Content() {
                        //重组触发器
                        var recomposeTrigger by remember { mutableStateOf(0L) }

                        controller.exeRecompositionBlock = {
                            recomposeTrigger = System.currentTimeMillis()
                        }

                        Box(
                            Modifier
                                .wrapContentSize()
                                .onSizeChanged {
                                    size = it
                                }
                        ) {
                            callbacks.value.contents(this, controller.getNeedPage()) { controller.refresh() }
                            recomposeTrigger
//                            Text(recomposeTrigger.toString(), color = Color.Transparent)
                            invalidate()
                        }
                    }

                    override fun dispatchDraw(canvas: Canvas?) {
                        if (width == 0 || height == 0) return
                        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas2 = Canvas(source)
                        super.dispatchDraw(canvas2)
                        canvas2.setBitmap(null)
                        controller.saveRenderedBitmap(source)
                    }
                }
            }
        )

        CompositionLocalProvider(
            LocalPTQBookPageViewConfig provides PTQBookPageViewConfig(pageColor = pageColor, disabled = state.disabled)
        ) {
            Box(
                Modifier
                    .width(size.width.dp)
                    .height(size.height.dp)
                    .align(Alignment.Center)
                    .clipToBounds()
            ) {
                PTQBookPageViewInner(controller = controller, callbacks = callbacks.value, onNext = {
                    if (state.currentPage == null && controller.currentPage < controller.totalPage - 1) {
                        controller.needBitmapAt(controller.currentPage + 1)
                        callbacks.value.onPageWantToChange(controller.currentPage, true, true)
                    } else {
                        callbacks.value.onPageWantToChange(controller.currentPage, true, controller.currentPage < controller.totalPage - 1)
                    }
                }, onPrevious = {
                    if (state.currentPage == null && controller.currentPage > 0) {
                        controller.needBitmapAt(controller.currentPage - 1)
                        callbacks.value.onPageWantToChange(controller.currentPage, false, true)
                    } else {
                        callbacks.value.onPageWantToChange(controller.currentPage, false, controller.currentPage > 0)
                    }
                })
            }
        }
    }
}