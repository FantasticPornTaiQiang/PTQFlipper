package ptq.mpga.ptqbookpageview.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.EmptyCoroutineContext

private const val TAG = "PTQBookPageView"

internal class PTQBookPageViewScopeImpl : PTQBookPageViewScope {
    var contents: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit = { _, _ -> }
    var onTurnPageRequest: (Int, Boolean, Boolean) -> Unit = { _, _, _ -> }
    var tapBehavior: ((leftUp: Point, rightDown: Point, touchPoint: Point) -> Boolean?)? = null
    var dragBehavior: ((rightDown: Point, initialTouchPoint: Point, lastTouchPoint: Point, isRightToLeftWhenStart: Boolean) -> Pair<Boolean, Boolean?>)? = null
    var responseDragWhen: ((rightDown: Point, startTouchPoint: Point, currentTouchPoint: Point) -> Boolean?)? = null

    override fun onTurnPageRequest(block: (currentPage: Int, nextOrPrevious: Boolean, success: Boolean) -> Unit) {
        onTurnPageRequest = block
    }

    override fun contents(block: @Composable BoxScope.(currentPage: Int, refresh: () -> Unit) -> Unit) {
        contents = block
    }

    override fun tapBehavior(block: (leftUp: Point, rightDown: Point, touchPoint: Point) -> Boolean?) {
        tapBehavior = block
    }

    override fun responseDragWhen(block: (rightDown: Point, startTouchPoint: Point, currentTouchPoint: Point) -> Boolean?) {
        responseDragWhen = block
    }

    override fun dragBehavior(block: (rightDown: Point, initialTouchPoint: Point, lastTouchPoint: Point, isRightToLeftWhenStart: Boolean) -> Pair<Boolean, Boolean?>) {
        dragBehavior = block
    }
}

/**
 * @param modifier 修饰符[支持以padding方式设置组件位置和大小，或以height和width方式设置大小，不支持offset和size修饰符]
 * @param config 组件配置，请使用[rememberPTQBookPageViewConfig]
 * @param state 组件状态，请使用[rememberPTQBookPageViewState]
 * @param ptqBookPageViewScope 翻页器提供的各类回调 [PTQBookPageViewScope]
 */
@Composable
fun PTQBookPageView(
    modifier: Modifier = Modifier, config: PTQBookPageViewConfig = PTQBookPageViewConfig(), state: PTQBookPageViewState, ptqBookPageViewScope: PTQBookPageViewScope.() -> Unit
) {
    Box(
        modifier.fillMaxSize()
    ) {
        val controller by remember {
            mutableStateOf(PTQBookPageBitmapController(state.pageCount))
        }

        DisposableEffect(Unit) {
            onDispose {
                controller.destroy()
            }
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
                                .fillMaxSize()
                                .align(Alignment.Center)) {
                            callbacks.value.contents(this, controller.getNeedPage()) { controller.refresh() }
                            recomposeTrigger
                            invalidate()
                        }
                    }

                    override fun dispatchDraw(canvas: Canvas?) {
                        /**
                         * 复用canvas、bitmap，且把流程控制在BitmapController内部，dispatchDraw只负责dispatchDraw
                         * @since v1.1.0
                         */
                        controller.renderThenSave(width, height) {
                            super.dispatchDraw(it)
                        }
                    }
                }
            }
        )

        //貌似必须包裹在CompositionLocalProvider，否则就会不断重组，没想通为什么
        CompositionLocalProvider(
            LocalPTQBookPageViewConfig provides config
        ) {
            var position by remember { mutableStateOf(Rect.Zero) }

            Box(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .clipToBounds()
                    .onGloballyPositioned {
                        position = it.boundsInRoot()
                    }
            ) {
                PTQBookPageViewInner(
                    bounds = position,
                    controller = controller,
                    callbacks = callbacks.value,
                    onNext = {
                        with(controller) {
                            if (state.currentPage == null && currentPage < totalPage - 1) {
                                needBitmapAt(currentPage + 1)
                                callbacks.value.onTurnPageRequest(currentPage, true, true)
                            } else {
                                callbacks.value.onTurnPageRequest(currentPage, true, currentPage < totalPage - 1)
                            }
                        }
                    }, onPrevious = {
                        with(controller) {
                            if (state.currentPage == null && currentPage > 0) {
                                needBitmapAt(currentPage - 1)
                                callbacks.value.onTurnPageRequest(currentPage, false, true)
                            } else {
                                callbacks.value.onTurnPageRequest(currentPage, false, currentPage > 0)
                            }
                        }
                    }
                )
            }
        }

    }
}


