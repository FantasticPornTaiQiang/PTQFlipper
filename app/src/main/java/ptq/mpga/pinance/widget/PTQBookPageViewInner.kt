package ptq.mpga.pinance.widget

import android.graphics.*
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ptq.mpga.pinance.widget.Line.Companion.k
import ptq.mpga.pinance.widget.Line.Companion.theta
import kotlin.math.*

private const val TAG = "PTQBookPageViewInner"

//带ratio的要乘屏幕比例
private const val stateTightMinWERatio = 1 / 8f //进入tight状态的最小WE长度
private const val minTheta = ((137f * PI) / 180).toFloat() //最小θ，弧度制
private const val stateTightDragUpThetaRate = 0.1f //tight状态向上拉扯的速率，该值越大则拉扯越快
private const val stateTightDragRightScreenDistanceRatio = 1 / 40f //tight状态手指移到屏幕右侧多少时到达最终态
private const val maxDragXRatio = stateTightDragRightScreenDistanceRatio //手指最大X，和上个变量一样
private const val minWxRatio = 1 / 25f //W.x的最小值

private const val animStartTimeout = 75//动画在手指拖动多久后开始，此值不宜过大，会造成不准确
private const val animEnterDuration = 100 //入动画时间，此值不宜过大，会造成不准确
private const val animExitDuration = animEnterDuration //出动画时间

private const val tapYDeltaRatio = 1 / 170f //点击翻页时，y方向偏移，不宜过大

private const val shadowThreshold = 22f //阴影1、2阈值
private const val shadowPart3to1Ratio = 1.5f //阴影3与阴影1的宽度比
private const val shadow3VerticalThreshold = 50 //处理当接近垂直时，底层绘制api不正常工作的问题

private val shadow12Color = Color.Black.copy(alpha = 0.3f) //12部分阴影的颜色
private val shadow3Color = Color.Black.copy(alpha = 0.65f) //3部分阴影的颜色
private val lustreColor = Color.Black.copy(alpha = 0.02f) //光泽部分阴影的颜色

//最好设置为统一比例
private const val lustreStartMaxDistance = 9f //光泽右侧最大距离
private const val lustreStartMinDistance = 6f //光泽右侧最小距离，即当WEmin时的距离
private const val lustreEndMaxDistance = 9f //光泽左侧最大距离
private const val lustreEndMinDistance = 6f //光泽左侧最小距离，即当WEmin时的距离
private const val lustreEndShadowMaxWidth = 24f //光泽左侧阴影最大宽度
private const val lustreEndShadowMinWidth = 16f //光泽左侧阴影最小宽度，即当WEmin时的宽度

private const val distortionInterval = 12 //扭曲的间隔

private enum class PageState {
    Loose, WMin, ThetaMin, Tight
}

/**
 * 翻页器组件的流程状态
 */
private enum class State {
    Idle, EnterAnimStart, Draggable, ExitAnimStart, ExitAnimPreStart
}

/**
 * @param callbacks 内容
 * @param controller 控制器
 * @param onNext 企图下一页时调用（当处于最后一页仍想翻下一页也会触发onNext）
 * @param onPrevious 企图上一页时调用（当处于第一页仍想翻下一页也会触发onPrevious）
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun PTQBookPageViewInner(
    controller: PTQBookPageBitmapController,
    callbacks: PTQBookPageViewScopeImpl,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val localConfig = LocalPTQBookPageViewConfig.current

    //配置
    val config = rememberUpdatedState {
        return@rememberUpdatedState localConfig
    }

    //组件宽高和原点O
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val dragXRange by remember { mutableStateOf(FloatRange(0f, screenWidth * (1 - maxDragXRatio))) }

    //组件左上角
    val absO by remember { mutableStateOf(Point(0f, 0f)) }
    //Tap回调中的组件左上角
    val leftUpOnTap by remember(screenHeight, absO) { mutableStateOf(Point(absO.x, tapYDeltaRatio * screenHeight)) }
    //组件右下角
    val absC by remember(absO, screenHeight, screenWidth) { mutableStateOf(absO + Point(screenWidth, screenHeight)) }

    //位于组件一半高度的直线，用于处理翻转，绝对坐标系
    val lBCPerpendicularBisector by remember { mutableStateOf(Line(0f, screenHeight / 2)) }

    //拖动事件，包括原触点，增量，现触点
    var curDragEvent by remember { mutableStateOf(DragEvent(absO.copy(), absO.copy())) }
    //当前状态
    var pageState by remember { mutableStateOf(PageState.Loose) }
    //要绘制的所有点，绝对系
    var allPoints by remember { mutableStateOf(AllPoints.default(absO)) }
    //当前翻转状态
    var upsideDown by remember { mutableStateOf(false) }

    /**
     * 点击和拖动时起手是右往左还是左往右（受[PTQBookPageViewScope.tapBehavior]和[PTQBookPageViewScope.dragBehavior]控制）
     */
    var isRightToLeftWhenStart by remember { mutableStateOf(true) }
    //应该是下一页还是上一页，因为要等动画结束才能算完全翻完页，所以回调要在动画结束之后，因此需要一个变量记录一下状态
    var isNextOrPrevious by remember { mutableStateOf<Boolean?>(null) }
    /**
     * 拖动起始点（起始动画之前，用户的最开始触摸点），用于[PTQBookPageViewScope.dragBehavior]
     */
    var dragInitialPoint by remember { mutableStateOf(absO.copy()) }

    //倾角θ
    var theta by remember { mutableStateOf(0f) }
    //fixedValue
    var f by remember { mutableStateOf(0f) }

    //流程状态
    var state: State by remember { mutableStateOf(State.Idle) }
    //入动画还未结束就松手了
    var exitPreStartState by remember { mutableStateOf(false) }
    //点击翻页则匀一下进出时间
    var animDuration by remember { mutableStateOf(arrayOf(animEnterDuration, animExitDuration)) }

    //（在进入State.Draggable之前）是否被responseDragWhen终止，为true意味着无任何响应（不执行退出动画）
    var interruptedInDrag by remember { mutableStateOf(false) }

    //动画的初始和最终状态
    var animStartAndEndPoint by remember { mutableStateOf(DragEvent(absO.copy(), absO.copy())) }
    //动画的上一个点origin（这里的last是“上一个”的意思，不是“最后”的意思）
    var animLastPoint by remember { mutableStateOf(absO.copy()) }
    //定时，拖动多久后开始播放动画
    var time by remember { mutableStateOf(0L) }

    //页面颜色
    val pageColor by remember { derivedStateOf { config.value().pageColor } }
    //12区域阴影颜色
    val nativeShadow12Color by remember { mutableStateOf(android.graphics.Color.toArgb(shadow12Color.value.toLong())) }
    //3区阴影颜色
    val nativeShadow3Color by remember { mutableStateOf(android.graphics.Color.toArgb(shadow3Color.value.toLong())) }
    //光泽阴影颜色
    val nativeLustreColor by remember { mutableStateOf(android.graphics.Color.toArgb(lustreColor.value.toLong())) }
    //页面颜色
    val nativePageColor by remember { derivedStateOf { android.graphics.Color.toArgb(pageColor.value.toLong()) } }
    //透明色
    val nativeTransparentColor by remember { mutableStateOf(android.graphics.Color.toArgb(Color.Transparent.value.toLong())) }

    //退出动画lambda
    val exeExitAnim = rememberUpdatedState {
        val startPoint = if (!upsideDown) allPoints.J else allPoints.J.getSymmetricalPointAbout(lBCPerpendicularBisector)
        f = maxOf(0f, allPoints.J.distanceTo(allPoints.H))
        val y = if (!upsideDown) screenHeight - f else f

        val dragBehavior = callbacks.dragBehavior

        if (dragBehavior == null) {
            val isFingerAtRight = curDragEvent.currentTouchPoint.x > 0.5f * screenWidth

            //记录是下一页还是前一页，用于动画结束后触发onNext/onPrevious回调
            if (isFingerAtRight && !isRightToLeftWhenStart) {
                isNextOrPrevious = false
            }

            if (!isFingerAtRight && isRightToLeftWhenStart) {
                isNextOrPrevious = true
            }

            animStartAndEndPoint = DragEvent(startPoint.copy(), if (isFingerAtRight) Point(dragXRange.end, y) else Point(-dragXRange.end / 2, y))
        } else {
            val (isLeftLeave, isNext) = dragBehavior(absO, absC, dragInitialPoint, curDragEvent.currentTouchPoint, isRightToLeftWhenStart)

            isNext?.let {
                isNextOrPrevious = it
            }

            animStartAndEndPoint = DragEvent(startPoint.copy(), if (!isLeftLeave) Point(dragXRange.end, y) else Point(-dragXRange.end / 2, y))
        }

        animLastPoint = startPoint.copy()
        pageState = PageState.Loose
        state = State.ExitAnimStart
    }

    //动画
    val animFloatRatio by animateFloatAsState(targetValue = if (state == State.EnterAnimStart || state == State.ExitAnimStart) 1f else 0f, finishedListener = {
        //动画结束时
        when (state) {
            State.EnterAnimStart -> {
                state = if (exitPreStartState) State.ExitAnimPreStart else State.Draggable
            }
            State.ExitAnimStart -> {
                //所有流程的最后，所有状态重置
                curDragEvent = DragEvent(absO.copy(), absO.copy())
                state = State.Idle
                exitPreStartState = false
                allPoints = AllPoints.default(absO)
                upsideDown = false
                pageState = PageState.Loose
                time = 0L
                theta = 0f
                f = 0f
                animLastPoint = absO.copy()
                animStartAndEndPoint = DragEvent(absO.copy(), absO.copy())
                animDuration = arrayOf(animEnterDuration, animExitDuration)
                if (isNextOrPrevious == true) onNext() else if (isNextOrPrevious == false) onPrevious()
                isNextOrPrevious = null
                interruptedInDrag = false
                dragInitialPoint = absO.copy()
            }
            State.ExitAnimPreStart -> { //确保animFloatRatio回到0f
                exeExitAnim.value()
            }
            else -> {
            }
        }
    }, animationSpec = TweenSpec(easing = LinearEasing, durationMillis = if (state == State.EnterAnimStart) animDuration[0] else if (state == State.ExitAnimStart) animDuration[1] else 0))

    //拖动结束lambda
    val onDragEnd = rememberUpdatedState {
        when (state) {
            //正常状态下松手
            State.Draggable -> {
                exeExitAnim.value()
            }
            //如果正处于进入动画时松手了，则先进入预退出状态
            State.EnterAnimStart -> {
                exitPreStartState = true
            }
            //处理第一页或者最后一页（第一页或者最后一页时，所有流程的最后）
            State.Idle -> {
                val dragBehavior = callbacks.dragBehavior

                if (dragBehavior == null) {
                    val isFingerAtRight = curDragEvent.currentTouchPoint.x > 0.5f * screenWidth

                    if (isFingerAtRight && !isRightToLeftWhenStart) {
                        isNextOrPrevious = false
                    }

                    if (!isFingerAtRight && isRightToLeftWhenStart) {
                        isNextOrPrevious = true
                    }

                } else {
                    val (_, isNext) = dragBehavior(absO, absC, dragInitialPoint, curDragEvent.currentTouchPoint, isRightToLeftWhenStart)

                    isNext?.let {
                        isNextOrPrevious = it
                    }
                }

                if (isNextOrPrevious == true) onNext() else if (isNextOrPrevious == false) onPrevious()
                isNextOrPrevious = null
            }
            else -> {}
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { offset: Offset ->
                if (state != State.Idle || !dragXRange.contains(offset.x) || !controller.isRenderOk() || config.value().disabled) {
                    return@detectTapGestures
                }

                val touchPoint = offset.toPoint
                val tapYDelta = leftUpOnTap.y

                //最顶上预留tapYDelta的距离
                if (touchPoint.y > tapYDelta) {
                    val onTapBehavior = callbacks.tapBehavior

                    val isLeftToRight = if (onTapBehavior == null) {
                        touchPoint.x < 0.5f * screenWidth
                    } else {
                        val result = onTapBehavior(leftUpOnTap.copy(), absC.copy(), touchPoint.copy())

                        if (result == null) {
                            return@detectTapGestures
                        } else {
                            !result
                        }
                    }

                    if (controller.currentPage >= controller.totalPage - 1 && !isLeftToRight) {
                        onNext()
                        return@detectTapGestures
                    }

                    if (controller.currentPage <= 0 && isLeftToRight) {
                        onPrevious()
                        return@detectTapGestures
                    }

                    val startPoint = Point(x = if (isLeftToRight) -dragXRange.end * 0.5f else dragXRange.end, y = if (isLeftToRight) touchPoint.y - tapYDelta else touchPoint.y)
                    f = screenHeight - touchPoint.y.absoluteValue
                    val endPoint = Point(x = if (isLeftToRight) dragXRange.end else -dragXRange.end * 0.5f, y = if (isLeftToRight) touchPoint.y else touchPoint.y - tapYDelta)
                    animLastPoint = startPoint.copy()
                    animStartAndEndPoint = DragEvent(startPoint.copy(), endPoint.copy())
                    pageState = PageState.Loose
                    animDuration = arrayOf(animEnterDuration, animEnterDuration + animExitDuration)
                    state = State.ExitAnimStart

                    isRightToLeftWhenStart = !isLeftToRight
                    isNextOrPrevious = !isLeftToRight
                }
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset: Offset ->
                    if (state != State.Idle || !dragXRange.contains(offset.x) || !controller.isRenderOk() || config.value().disabled) {
                        return@detectDragGestures
                    }

                    val touchPoint = offset.toPoint
                    dragInitialPoint = offset.toPoint
                    pageState = PageState.Loose
                    interruptedInDrag = false
                    curDragEvent = DragEvent(touchPoint, touchPoint)
                    f = screenHeight - touchPoint.y.absoluteValue
                    animStartAndEndPoint = animStartAndEndPoint.copy(originTouchPoint = touchPoint)
                    time = System.currentTimeMillis()
                },
                onDrag = { _: PointerInputChange, dragAmount: Offset ->
                    if (!controller.isRenderOk() || config.value().disabled) {
                        return@detectDragGestures
                    }

                    val cur = (curDragEvent.currentTouchPoint + dragAmount.toPoint)
                    curDragEvent = DragEvent(curDragEvent.currentTouchPoint.copy(), cur)

                    if (!dragXRange.contains(cur.x)) {
                        onDragEnd.value()
                        return@detectDragGestures
                    }

                    if (state == State.Idle) {
                        if (System.currentTimeMillis() - time > animStartTimeout) {
                            if (animStartAndEndPoint
                                    .directionToOInCoordinateSystem()
                                    .isIn(DragDirection.up, DragDirection.down, DragDirection.static)
                            ) {
                                return@detectDragGestures
                            }

                            animStartAndEndPoint = animStartAndEndPoint.copy(currentTouchPoint = cur)

                            val responseDragWhen = callbacks.responseDragWhen

                            //手指从右向左翻，还是从左向右
                            val isRightToLeft = if (responseDragWhen == null) {
                                animStartAndEndPoint.currentTouchPoint.x < animStartAndEndPoint.originTouchPoint.x
                            } else {
                                responseDragWhen(absO.copy(), absC.copy(), animStartAndEndPoint.originTouchPoint.copy(), cur.copy())
                            }

                            if (isRightToLeft == null) {
                                interruptedInDrag = true
                                return@detectDragGestures
                            }

                            //如果是第一页或者最后一页，直接return，之后松手就会触发onDragEnd，且state=State.Idle
                            if ((isRightToLeft && controller.currentPage >= controller.totalPage - 1) ||
                                (!isRightToLeft && controller.currentPage <= 0)
                            ) {
                                return@detectDragGestures
                            }

                            animStartAndEndPoint.originTouchPoint.x = if (isRightToLeft) dragXRange.end else dragXRange.start
                            isRightToLeftWhenStart = isRightToLeft

                            //默认向上翻，如果向下则颠倒
                            val isUpToDown = animStartAndEndPoint.currentTouchPoint.y > animStartAndEndPoint.originTouchPoint.y
                            if (isUpToDown) {
                                upsideDown = true
                                f = screenHeight - f
                                Log.d(TAG, "PTQPageFlipper:  upsdiedown1 $upsideDown")
                            }
                            animLastPoint = animStartAndEndPoint.originTouchPoint.copy()
                            state = State.EnterAnimStart
                        }
                    }

                    if (state == State.Draggable) {
                        val dragEvent = if (upsideDown) curDragEvent.getSymmetricalDragEventAbout(lBCPerpendicularBisector) else curDragEvent

                        if (pageState == PageState.Tight) {
                            val newTheta = onDragWhenTightState(absO.copy(), f, theta, dragEvent, screenHeight, screenWidth)
                            theta = newTheta
                        }
                    }
                },
                onDragEnd = {
                    if (!interruptedInDrag) {
                        onDragEnd.value()
                    }
                }
            )
        }
        .background(color = pageColor)
    ) {
        if (state == State.Idle) {
            callbacks.contents(this, controller.currentPage) {}
        } else {
            //如果正在动画，则使用动画的animDragEvent，否则使用拖动的
            var dragEvent = when (state) {
                State.EnterAnimStart, State.ExitAnimStart -> {
                    val curPoint = animStartAndEndPoint.originTouchPoint + animStartAndEndPoint.dragDelta * animFloatRatio
                    val animDragEvent = DragEvent(animLastPoint, curPoint)
                    animLastPoint = curPoint
                    animDragEvent
                }
                State.Draggable -> {
                    curDragEvent
                }
                else -> {
                    null
                }
            }

            //计算本轮点坐标，如果有状态变化，则舍弃本轮（直接使用上一轮的allPoints）
            val newAllPoints = if (dragEvent != null && !dragEvent.isUnmoved) {
                dragEvent = if (upsideDown) dragEvent.getSymmetricalDragEventAbout(lBCPerpendicularBisector) else dragEvent

                when (pageState) {
                    PageState.Loose -> {
                        buildStateLoose(absO.copy(), dragEvent, Point(screenWidth, screenHeight), f, upsideDown, state) { newState, newTheta, newEmaxInThetaMin, newUpsideDown ->
                            newUpsideDown?.let {
                                Log.d(TAG, "PTQPageFlipper:  upsdiedown2 $it")
                                upsideDown = it
                                f = screenHeight - f
                                return@buildStateLoose
                            }
                            newState?.let { pageState = it }
                            newTheta?.let { theta = it }
                            Log.d(TAG, "PTQPageFlipper: state $pageState start")
                        }
                    }
                    PageState.WMin -> {
                        buildStateWMin(absO.copy(), dragEvent, Point(screenWidth, screenHeight), f) { newState, newTheta ->
                            theta = newTheta
                            pageState = newState
                            Log.d(TAG, "PTQPageFlipper: state $pageState start")
                        }
                    }
                    PageState.ThetaMin -> {
                        buildStateThetaMin(absO.copy(), dragEvent, Point(screenWidth, screenHeight), f) { newState, newTheta ->
                            theta = newTheta
                            pageState = newState
                            Log.d(TAG, "PTQPageFlipper: state $pageState start")
                        }
                    }
                    PageState.Tight -> {
                        buildStateTight(absO.copy(), Point(screenWidth, screenHeight), theta, dragEvent, f) { newState ->
                            pageState = newState
                            Log.d(TAG, "PTQPageFlipper: state $newState start")
                        }
                    }
                }
            } else {
                null
            }

            val distortBitmap = controller.getBitmapCurrent(if (isRightToLeftWhenStart) 1 else 0)
            val backgroundBitmap = controller.getBitmapCurrent(if (isRightToLeftWhenStart) 2 else 1)
            val currentBitmap = if (isRightToLeftWhenStart) distortBitmap else backgroundBitmap

            val nonNullAllPoints = newAllPoints ?: allPoints

            var upsideDownAllPoints = if (upsideDown) nonNullAllPoints.getSymmetricalPointAboutLine(lBCPerpendicularBisector) else nonNullAllPoints

            var (distortedEdges, distortedVertices, meshCounts) = upsideDownAllPoints.toCoordinateSystem(absO).buildDistortionPoints(distortBitmap, absO, upsideDown)

            //如果newAllPoint为空，或者页面完全垂直，则舍弃本轮，使用上一轮的点重新计算
            if (distortedEdges[0].isNotEmpty()) {
                if (newAllPoints != null) {
                    allPoints = newAllPoints
                }
            } else {
                if (newAllPoints != null) {
                    //newAllPoints不为null意味着使用的是本轮的点计算的，因此重算一次，如果为null则已经用的是allPoints算的了，不需要重算
                    upsideDownAllPoints = if (upsideDown) allPoints.getSymmetricalPointAboutLine(lBCPerpendicularBisector) else allPoints
                    val lastAll = upsideDownAllPoints.toCoordinateSystem(absO).buildDistortionPoints(distortBitmap, absO, upsideDown)
                    distortedEdges = lastAll.first
                    distortedVertices = lastAll.second
                    meshCounts = lastAll.third
                }
            }

            val (paths, shadowPaths, shaderControlPointPairs, shadow12Width) = upsideDownAllPoints.buildPath(distortedEdges, upsideDown)

            Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
                drawIntoCanvas {
                    val paint = Paint()
                    val frameworkPaint = paint.asFrameworkPaint()
                    paint.isAntiAlias = true
                    frameworkPaint.isAntiAlias = true
                    val nativeCanvas = it.nativeCanvas
                    //注意图层绘制顺序

                    //点还没计算出来就只画当前
                    if (upsideDownAllPoints.C.x == upsideDownAllPoints.O.x) {
                        nativeCanvas.drawBitmap(currentBitmap, absO.x, absO.y, frameworkPaint)
                        return@drawIntoCanvas
                    }

                    //画下一页
                    nativeCanvas.drawBitmap(backgroundBitmap, absO.x, absO.y, frameworkPaint)

                    //画阴影区域3
                    frameworkPaint.shader =
                        LinearGradient(
                            shaderControlPointPairs[2].first.x,
                            shaderControlPointPairs[2].first.y,
                            shaderControlPointPairs[2].second.x,
                            shaderControlPointPairs[2].second.y,
                            nativeShadow3Color,
                            nativeTransparentColor,
                            Shader.TileMode.CLAMP
                        )
                    it.drawPath(shadowPaths[2], paint)
                    it.clipPath(paths[2], ClipOp.Difference)

                    //画当前页
                    frameworkPaint.shader = null
                    frameworkPaint.color = nativePageColor
                    it.drawPath(paths[0], paint)
                    nativeCanvas.drawBitmapMesh(distortBitmap, meshCounts[0], meshCounts[1], distortedVertices, 0, null, 0, frameworkPaint)

                    //画阴影区域1
                    frameworkPaint.shader = LinearGradient(
                        shaderControlPointPairs[0].first.x,
                        shaderControlPointPairs[0].first.y,
                        shaderControlPointPairs[0].second.x,
                        shaderControlPointPairs[0].second.y,
                        nativeShadow12Color,
                        nativeTransparentColor,
                        Shader.TileMode.CLAMP
                    )
                    it.drawPath(shadowPaths[0], paint)

                    //画阴影区域2
                    frameworkPaint.shader = LinearGradient(
                        shaderControlPointPairs[1].first.x,
                        shaderControlPointPairs[1].first.y,
                        shaderControlPointPairs[1].second.x,
                        shaderControlPointPairs[1].second.y,
                        nativeShadow12Color,
                        nativeTransparentColor,
                        Shader.TileMode.CLAMP
                    )
                    it.drawPath(shadowPaths[1], paint)

                    //画阴影区域4（圆弧）
                    frameworkPaint.shader =
                        RadialGradient(shaderControlPointPairs[0].first.x, shaderControlPointPairs[0].first.y, shadow12Width, nativeShadow12Color, nativeTransparentColor, Shader.TileMode.CLAMP)
                    it.drawPath(shadowPaths[3], paint)

                    //画当前页背面
                    frameworkPaint.shader = null
                    frameworkPaint.color = nativePageColor
                    it.drawPath(paths[1], paint)

                    //画光泽左侧阴影
                    frameworkPaint.shader = LinearGradient(
                        shaderControlPointPairs[3].first.x,
                        shaderControlPointPairs[3].first.y,
                        shaderControlPointPairs[3].second.x,
                        shaderControlPointPairs[3].second.y,
                        nativeLustreColor,
                        nativeTransparentColor,
                        Shader.TileMode.CLAMP
                    )
                    it.drawPath(shadowPaths[4], paint)

                    //画光泽右侧阴影
                    frameworkPaint.shader = LinearGradient(
                        shaderControlPointPairs[4].first.x,
                        shaderControlPointPairs[4].first.y,
                        shaderControlPointPairs[4].second.x,
                        shaderControlPointPairs[4].second.y,
                        nativeLustreColor,
                        nativeLustreColor,
                        Shader.TileMode.CLAMP
                    )
                    it.drawPath(shadowPaths[5], paint)
                }
                //加强一下轮廓
                drawPath(paths[1], shadow12Color.copy(alpha = 0.25f), style = Stroke(width = 1f))
            })
        }
    }
}

private inline fun buildStateLoose(
    absO: Point,
    dragEvent: DragEvent,
    absC: Point,
    f: Float,
    upsideDown: Boolean,
    state: State,
    changeState: (newPageState: PageState?, theta: Float?, EmaxInThetaMin: Float?, upsideDown: Boolean?) -> Unit
): AllPoints? {
    val (theta, points) = algorithmStateLoose(absO, dragEvent.originTouchPoint, absC, f)

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val lHF = Line.withTwoPoints(points.H, dragEventCoordinate.originTouchPoint)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)
    val flipDragDirection = dragEventCoordinate.directionToOInCoordinateSystem()
    val Ex = Line.withKAndOnePoint(-1 / Line.withTwoPoints(points.C, points.H).k, points.C..points.H).x(points.C.y)
    val kJH = Line.withTwoPoints(points.J, points.H).k

    if (dragDirection == DragDirection.static || kJH.isNaN()) return null

    if (state == State.EnterAnimStart || state == State.Draggable) {
        when {
            //翻转
            kJH <= 0 && flipDragDirection.isIn(DragDirection.down, DragDirection.rightDown, DragDirection.leftDown, DragDirection.left, DragDirection.right) -> {
                changeState(null, null, null, !upsideDown)
                return null
            }
            points.W.x < minWxRatio * points.C.x && dragDirection.isIn(DragDirection.up, DragDirection.rightUp, DragDirection.leftUp, DragDirection.leftDown) -> {
                changeState(PageState.WMin, theta, null, null)
                return null
            }
            theta < minTheta && dragDirection.isIn(DragDirection.up, DragDirection.rightUp, DragDirection.right, DragDirection.rightDown) -> {
                changeState(PageState.ThetaMin, minTheta, Ex, null)
                return null
            }
        }
    }

    return points.toAbsSystem(absO)
}

private fun algorithmStateLoose(absO: Point, absR: Point, absC: Point, f: Float): Pair<Float, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val A = Point(O.x, C.y)
    val B = Point(C.x, O.y)
    val R = absR.inCoordinateSystem(absO)

    val Rf = Point(C.x, C.y + f)
    val k = (C.x - R.x) / (R.y - Rf.y)

    val lWZ = Line.withKAndOnePoint(k, R)
    val W = Point(lWZ.x(C.y), C.y)
    val Z = Point(C.x, lWZ.y(C.x))

    val lEF = Line.withKAndOnePoint(k, R..Rf)
    val E = Point(lEF.x(C.y), C.y)
    val F = Point(C.x, lEF.y(C.x))

    val S = W..E
    val T = Z..F

    val lCH = Line.withKAndOnePoint(-1 / k, C)

    val P = lEF.intersectAt(lCH)
    val H = Point(2 * P.x - C.x, 2 * P.y - C.y)

    val lHE = Line.withTwoPoints(E, H)
    val lHF = Line.withTwoPoints(F, H)
    val lST = Line.withKAndOnePoint(k, S)
    val J = R.copy()
    val I = lHE.intersectAt(lWZ)
    val U = lHE.intersectAt(lST)
    val V = lHF.intersectAt(lST)

    val M = U..S
    val N = T..V

    val allPoints = AllPoints(O, A, B, C, H, I, J, M, N, S, T, U, V, W, Z)

    return Pair(lCH.theta(), allPoints)
}

private inline fun buildStateWMin(absO: Point, dragEvent: DragEvent, absC: Point, f: Float, changeState: (newPageState: PageState, theta: Float) -> Unit): AllPoints? {
    val (theta, WE, points) = algorithmStateWMin(absO, dragEvent.originTouchPoint, absC, f)
    val R = dragEvent.currentTouchPoint.inCoordinateSystem(absO)
    val looseWx = Line.withKAndOnePoint((points.C.x - R.x) / (R.y - Point(points.C.x, points.C.y + f).y), R).x(points.C.y)
    val minWE = stateTightMinWERatio * points.C.x

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val lHF = Line.withTwoPoints(points.H, dragEventCoordinate.originTouchPoint)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)

    when {
        WE < minWE && dragDirection.isIn(DragDirection.up, DragDirection.rightUp, DragDirection.right, DragDirection.leftUp) -> {
            changeState(PageState.Tight, maxOf(minTheta, theta))
            return null
        }
        (looseWx >= minWxRatio * points.C.x) && dragDirection.isIn(DragDirection.leftDown, DragDirection.down, DragDirection.rightDown, DragDirection.left) -> {
            changeState(PageState.Loose, maxOf(minTheta, theta))
            return null
        }
        theta < minTheta && dragDirection.isIn(DragDirection.right, DragDirection.up, DragDirection.rightUp, DragDirection.rightDown) -> {
            changeState(PageState.ThetaMin, minTheta)
            return null
        }
    }

    return points.toAbsSystem(absO)
}

private fun algorithmStateWMin(absO: Point, absTouchPoint: Point, absC: Point, f: Float): Triple<Float, Float, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val A = Point(O.x, C.y)
    val B = Point(C.x, O.y)
    val R = absTouchPoint.inCoordinateSystem(absO)
    val W = Point(minWxRatio * C.x, C.y)

    val Rf = Point(C.x, C.y + f)
    val k = (C.x - R.x) / (R.y - Rf.y)

    val Z = Point(C.x, k * (C.x - W.x) + C.y)

    val lEF = Line.withKAndOnePoint(k, R..Rf)
    val E = Point(lEF.x(C.y), C.y)
    val F = Point(C.x, lEF.y(C.x))

    val S = W..E
    val T = Z..F

    val lCH = Line.withKAndOnePoint(-1 / k, C)

    val P = lEF.intersectAt(lCH)
    val H = Point(2 * P.x - C.x, 2 * P.y - C.y)

    val lHE = Line.withTwoPoints(E, H)
    val lHF = Line.withTwoPoints(F, H)
    val lST = Line.withKAndOnePoint(k, S)
    val lWZ = Line.withKAndOnePoint(k, W)
    val I = lHE.intersectAt(lWZ)
    val J = lHF.intersectAt(lWZ)
    val U = lHE.intersectAt(lST)
    val V = lHF.intersectAt(lST)

    val M = U..S
    val N = T..V

    val allPoints = AllPoints(O, A, B, C, H, I, J, M, N, S, T, U, V, W, Z)

    return Triple(lCH.theta(), E.x - W.x, allPoints)
}

private inline fun buildStateThetaMin(absO: Point, dragEvent: DragEvent, absC: Point, f: Float, changeState: (newPageState: PageState, theta: Float) -> Unit): AllPoints? {
    val minWx = minWxRatio * absC.x
    val minWE = stateTightMinWERatio * absC.x

    val (WE, points) = algorithmStateThetaMin(absO, dragEvent.originTouchPoint, absC, f)

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val lHF = Line.withTwoPoints(points.H, dragEventCoordinate.originTouchPoint)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)

    when {
        points.W.x <= minWx && WE <= minWE && dragDirection.isIn(DragDirection.rightUp, DragDirection.leftUp, DragDirection.up, DragDirection.right) -> {
            changeState(PageState.Tight, minTheta)
            return null
        }
        points.H.distanceTo(dragEventCoordinate.originTouchPoint) < f && points.W.x > minWx && dragDirection.isIn(
            DragDirection.left,
            DragDirection.leftDown,
            DragDirection.rightDown,
            DragDirection.down,
            DragDirection.leftUp
        ) -> {
            changeState(PageState.Loose, minTheta)
            return null
        }
        points.H.distanceTo(dragEventCoordinate.originTouchPoint) < f && points.W.x <= minWx && dragDirection.isIn(DragDirection.leftDown, DragDirection.left, DragDirection.leftUp) -> {
            changeState(PageState.WMin, minTheta)
            return null
        }
    }

    return points.toAbsSystem(absO)
}

private fun algorithmStateThetaMin(absO: Point, absTouchPoint: Point, absC: Point, f: Float): Pair<Float, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val A = Point(O.x, C.y)
    val B = Point(C.x, O.y)
    val R = absTouchPoint.inCoordinateSystem(absO)

    val minWx = minWxRatio * C.x
    val minWE = stateTightMinWERatio * C.x

    val looseWx = Line.withKAndOnePoint((C.x - R.x) / (R.y - Point(C.x, C.y + f).y), R).x(C.y)
    val isLoose = looseWx >= minWx

    val lCH = Line.withKAndOnePoint(k(minTheta), C)
    val k = -1 / lCH.k

    val G = Point(C.x, R.y + lCH.k * (C.x - R.x))

    val E = Point(Line.withKAndOnePoint(k, R..G).x(C.y), C.y)
    val F = Point(C.x, k * (C.x - E.x) + C.y)
    val lEF = Line.withKAndOnePoint(k, E)

    val P = lEF.intersectAt(lCH)
    val H = Point(2 * P.x - C.x, 2 * P.y - C.y)

    val isFingerOut = H.distanceTo(R) > f || R.distanceTo(Line.withKAndOnePoint(0f, C)) > f

    val Wx_ = if (isFingerOut) {
        R.x - (R.y - C.y) / k
    } else {
        if (isLoose) looseWx else minWx
    }
    val WE = (E.x - Wx_).run { maxOf(minWE, this) }

    val W = Point((E.x - WE).run { maxOf(this, minWx) }, C.y)
    val lWZ = Line.withKAndOnePoint(k, W)
    val Z = Point(C.x, lWZ.y(C.x))

    val S = W..E
    val T = Z..F

    val lHE = Line.withTwoPoints(E, H)
    val lHF = Line.withTwoPoints(F, H)
    val lST = Line.withKAndOnePoint(k, S)
    val J = lHF.intersectAt(lWZ)
    val I = lHE.intersectAt(lWZ)
    val U = lHE.intersectAt(lST)
    val V = lHF.intersectAt(lST)

    val M = U..S
    val N = T..V

    val allPoints = AllPoints(O, A, B, C, H, I, J, M, N, S, T, U, V, W, Z)

    return Pair(E.x - W.x, allPoints)
}

private fun onDragWhenTightState(absO: Point, f: Float, currentTheta: Float, absDragEvent: DragEvent, screenHeight: Float, screenWidth: Float): Float {
    val dragEvent = absDragEvent.inCoordinateSystem(absO)

    return when (dragEvent.directionToOInCoordinateSystem()) {
        DragDirection.right -> {
            val delta = getTightStateDeltaWhenRight(currentTheta, absDragEvent.currentTouchPoint.x, absDragEvent.dragDelta.x, screenWidth)
            currentTheta + delta
        }
        DragDirection.up -> {
            val delta = getTightStateDeltaWhenUp(currentTheta, absDragEvent.dragDelta.y)
            currentTheta + delta
        }
        //左下、下、左、左上、右下
        DragDirection.leftDown, DragDirection.down, DragDirection.left, DragDirection.leftUp, DragDirection.rightDown -> {
            val (_, delta) = getTightStateDeltaWhenBack(absO, f, currentTheta, absDragEvent, screenWidth, screenHeight)
            currentTheta + delta
        }
        //右上
        DragDirection.rightUp -> {
            val deltaRight = getTightStateDeltaWhenRight(currentTheta, absDragEvent.currentTouchPoint.x, absDragEvent.dragDelta.x, screenWidth)
            val deltaUp = getTightStateDeltaWhenUp(currentTheta, absDragEvent.dragDelta.y)
            val yToX = -absDragEvent.dragDelta.y / absDragEvent.dragDelta.x
            currentTheta + (yToX / (1 + yToX)) * deltaUp + (1 / (1 + yToX)) * deltaRight
        }
        else -> currentTheta
    }
}

private fun getTightStateDeltaWhenUp(currentTheta: Float, dragDeltaY: Float): Float {
    val thetaRange = FloatRange(maxOf(currentTheta, minTheta), minTheta)
    val newTheta = (dragDeltaY * stateTightDragUpThetaRate).toRad().run { thetaRange.constraints(this + currentTheta) }
    return newTheta - currentTheta
}

private fun getTightStateDeltaWhenRight(currentTheta: Float, currentFingerX: Float, dragDeltaX: Float, screenWidth: Float): Float {
    val dragRange = FloatRange(currentFingerX - dragDeltaX, screenWidth * (1 - stateTightDragRightScreenDistanceRatio))
    val thetaRange = FloatRange(maxOf(currentTheta, minTheta), minTheta)
    val newTheta = dragRange.linearMapping(currentFingerX, thetaRange).run { thetaRange.constraints(this) }
    return newTheta - currentTheta
}

private fun getTightStateDeltaWhenBack(absO: Point, f: Float, currentTheta: Float, dragEvent: DragEvent, screenWidth: Float, screenHeight: Float): Pair<Point, Float> {
    val C = Point(screenWidth, screenHeight).inCoordinateSystem(absO)
    val W = Point(minWxRatio * screenWidth, C.y)
    val E = Point(W.x + stateTightMinWERatio * C.x, C.y)
    val Rc = if (dragEvent.dragDelta.x == 0f) {
        val q = dragEvent.currentTouchPoint.x
        val a = 1f
        val b = -2 * C.y
        val c = C.y.pow(2) - f.pow(2) - (C.x - q) * (C.x + q - 2 * E.x)
        val RyEquation = QuadraticEquationWithOneUnknown(a, b, c)
        val Ry = RyEquation.solve().run {
            if (size < 2) return Pair(Point(Float.NaN, Float.NaN), 0f) else {
                maxOf(this[0].absoluteValue, this[1].absoluteValue)
            }
        }
        Point(q, Ry)
    } else {
        val touchLine = Line.withKAndOnePoint(-dragEvent.dragDelta.y / dragEvent.dragDelta.x, dragEvent.currentTouchPoint.inCoordinateSystem(absO))
        val m = touchLine.k
        val n = touchLine.b

        val a = 1 + m.pow(2)
        val b = 2 * (m * n - m * C.y - E.x)
        val c = n.pow(2) + C.y.pow(2) - f.pow(2) - 2 * n * C.y - C.x.pow(2) + 2 * C.x * E.x
        val RxEquation = QuadraticEquationWithOneUnknown(a, b, c)
        val Rx = RxEquation.solve().run {
            if (size < 2) return Pair(Point(Float.NaN, Float.NaN), 0f) else {
                maxOf(this[0], this[1])
            }
        }
        Point(Rx, m * Rx + n)
    }

    val finalK = (Rc.y - C.y + f) / (Rc.x + C.x - 2 * E.x)
    val finalTheta = theta(-1 / finalK)

    if (finalTheta < minTheta) {
        return Pair(Point(Float.NaN, Float.NaN), 0f)
    }

    val thetaRange = FloatRange(minOf(currentTheta, finalTheta), finalTheta)

    val originTouchPoint = dragEvent.originTouchPoint.inCoordinateSystem(absO)
    val fingerRange = FloatRange(0f, originTouchPoint.distanceTo(Rc))

    val newFingerValue = dragEvent.currentTouchPoint.inCoordinateSystem(absO).distanceTo(originTouchPoint)
    val newTheta = fingerRange.linearMappingWithConstraints(newFingerValue, thetaRange).run { thetaRange.constraints(this) }

    return Pair(Rc, newTheta - currentTheta)
}

private fun buildStateTight(absO: Point, absC: Point, theta: Float, dragEvent: DragEvent, f: Float, changeState: (newPageState: PageState) -> Unit): AllPoints? {
    val (lHF, allPoints) = algorithmStateTight(absO, absC, theta)

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)

    val Rc = dragEvent.currentTouchPoint.inCoordinateSystem(absO)

    if (Rc.isBelow(lHF) && dragDirection.isIn(DragDirection.rightDown, DragDirection.down, DragDirection.leftDown)) {
        return if (Rc.distanceTo(allPoints.H) >= f && dragDirection.isIn(DragDirection.rightDown, DragDirection.down, DragDirection.leftDown)) {
            changeState(PageState.ThetaMin)
            null
        } else {
            changeState(PageState.WMin)
            null
        }
    }

    return allPoints.toAbsSystem(absO)
}

private fun algorithmStateTight(absO: Point, absC: Point, theta: Float): Pair<Line, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val B = Point(C.x, O.y)
    val A = Point(O.x, C.y)

    val kCH = k(theta)
    val kEF = -1 / kCH

    val minWE = stateTightMinWERatio * C.x

    val W = Point(minWxRatio * C.x, C.y)
    val Z = Point(C.x, C.y + kEF * (C.x - W.x))

    val E = Point(W.x + minWE, C.y)
    val S = E..W

    val lCH = Line.withKAndOnePoint(kCH, C)
    val lWZ = Line.withKAndOnePoint(kEF, W)
    val lST = Line.withKAndOnePoint(kEF, S)
    val lEF = Line.withKAndOnePoint(kEF, E)

    val T = Point(C.x, lST.y(C.x))
    val F = Point(C.x, lEF.y(C.x))

    val P = lEF.intersectAt(lCH)
    val H = Point(2 * P.x - C.x, 2 * P.y - C.y)
    val lEH = Line.withTwoPoints(E, H)
    val lFH = Line.withTwoPoints(F, H)

    val I = lWZ.intersectAt(lEH)
    val U = lST.intersectAt(lEH)
    val J = lWZ.intersectAt(lFH)
    val V = lST.intersectAt(lFH)
    val M = U..S
    val N = V..T

    val allPoints = AllPoints(O, A, B, C, H, I, J, M, N, S, T, U, V, W, Z)

    return Pair(lFH, allPoints)
}

/**
 * 绝对系，根据点计算路径
 * @return 第一项为页面路径，第二项为阴影路径，第三项为阴影控制点，第四项为shadow12的宽度。
 * 第二项中的五个子项分别代表区域1、区域2、区域3、区域4（圆弧）、光泽左侧、光泽右侧；
 * 第三项中的四个子项分别代表区域1、区域2、区域3、光泽左侧、光泽右侧；
 * 第四项是区域12阴影宽度。
 */
private fun AllPoints.buildPath(distortedEdges: Array<List<Float>>, isUpsideDown: Boolean): PathResult {
    val (NVJ, ZTN, WSM, MUI) = distortedEdges

    //页面
    val thisPage = Path().apply {
        moveTo(W)
        connect(WSM)
        lineTo(M)
        lineTo(N)
        connect(ZTN, !isUpsideDown)
        lineTo(Z)
        lineTo(B)
        lineTo(O)
        lineTo(A)
        lineTo(W)
    }

    val thisPageBack = Path().apply {
        moveTo(M)
        lineTo(N)
        connect(NVJ, isUpsideDown)
        lineTo(H)
        connect(MUI, true)
        lineTo(M)
    }

    val nextPage = Path().apply {
        moveTo(C)
        lineTo(W)
        connect(WSM)
        lineTo(M)
        lineTo(N)
        connect(ZTN, !isUpsideDown)
        lineTo(Z)
        lineTo(C)
    }

    //阴影
    val lHF = Line.withTwoPoints(J, H)
    val lHE = Line.withTwoPoints(H, I)
    val WE = (S.x - W.x) * 2
    val WERange = FloatRange(0f, stateTightMinWERatio * (C.x - O.x))
    val shadow12Range = FloatRange(0f, shadowThreshold)
    val shadow12Width = WERange.linearMapping(WE, shadow12Range)
    val shadow3Width = shadow12Width * shadowPart3to1Ratio

    val H1 = H.getExtensionPointInAbs(lHF, shadow12Width)
    val lIParallel = Line.withKAndOnePoint(lHF.k, I)
    val I1 = I.getExtensionPointInAbs(lIParallel, shadow12Width)
    val lUParallel = Line.withKAndOnePoint(lHF.k, U)
    val U1 = U.getExtensionPointInAbs(lUParallel, shadow12Width)

    val H2 = H.getExtensionPointInAbs(lHE, shadow12Width)
    val lJParallel = Line.withKAndOnePoint(lHE.k, J)
    val J1 = J.getExtensionPointInAbs(lJParallel, shadow12Width)
    val lVParallel = Line.withKAndOnePoint(lHE.k, V)
    val V1 = V.getExtensionPointInAbs(lVParallel, shadow12Width)

    val lST = Line.withTwoPoints(S, T)
    val S1 = if (!lST.k.isNaN()) Point(S.x + shadow3Width / ((1 - 1 / (1 + lST.k * lST.k)).pow(0.5f)), C.y) else Point(S.x + shadow3Width, C.y)
    val T1 = if (!lST.k.isNaN()) Point(C.x, Line.withKAndOnePoint(lST.k, S1).y(C.x)) else Point(S1.x, O.y)

    val shadow1 = Path().apply {
        moveTo(H2)
        lineTo(J1)
        quadraticBezierTo(V1, N)
        quadraticBezierTo(V, J)
        lineTo(H)
        close()
    }

    //圆弧
    val shadow4 = Path().apply {
        moveTo(H1)
        //处理翻转
        if (C.y == 0f) {
            arcTo(Rect(H.toOffset, shadow12Width), lHF.theta().toDeg() + 180f, -90f, true)
        } else {
            arcTo(Rect(H.toOffset, shadow12Width), lHF.theta().toDeg(), 90f, true)
        }
        lineTo(H)
        close()
    }

    val shadow2 = Path().apply {
        moveTo(H)
        lineTo(H1)
        lineTo(I1)
        quadraticBezierTo(U1, M)
        quadraticBezierTo(U, I)
        close()
        op(this, nextPage, PathOperation.Difference)
    }

    val shadow3 = Path().apply {
        moveTo(W)
        lineTo(S1)
        //若接近垂直，则直接画成方形，否则画梯形
        if (((T1.y - O.y) / (C.y - O.y)).absoluteValue > shadow3VerticalThreshold) {
            lineTo(S1.copy(y = (C.y - O.y).absoluteValue - S1.y))
            lineTo(W.copy(y = (C.y - O.y).absoluteValue - W.y))
        } else {
            lineTo(T1)
            lineTo(Z)
        }
        close()
    }

    //光泽
    val lustreStartRange = FloatRange(0f, lustreStartMinDistance)
    val lustreEndRange = FloatRange(0f, lustreEndMinDistance)
    val lustreEndShadowWidthRange = FloatRange(0f, lustreEndShadowMinWidth)
    val lustreStartDistance = WERange.linearMapping(WE, lustreStartRange).run { minOf(this, lustreStartMaxDistance) }
    val lustreEndDistance = WERange.linearMapping(WE, lustreEndRange).run { minOf(this, lustreEndMaxDistance) }
    val lustreEndShadowWidth = WERange.linearMapping(WE, lustreEndShadowWidthRange).run { minOf(this, lustreEndShadowMaxWidth) }

    val S4 = if (!lST.k.isNaN()) Point(S.x - lustreStartDistance / ((1 - 1 / (1 + lST.k * lST.k)).pow(0.5f)), C.y) else Point(S.x - lustreStartDistance, C.y)
    val T4 = if (!lST.k.isNaN()) Point(C.x, Line.withKAndOnePoint(lST.k, S4).y(C.x)) else Point(S4.x, O.y)
    val S2 = if (!lST.k.isNaN()) Point(S4.x - lustreEndDistance / ((1 - 1 / (1 + lST.k * lST.k)).pow(0.5f)), C.y) else Point(S4.x - lustreEndDistance, C.y)
    val T2 = if (!lST.k.isNaN()) Point(C.x, Line.withKAndOnePoint(lST.k, S2).y(C.x)) else Point(S2.x, O.y)
    val S3 = if (!lST.k.isNaN()) Point(S2.x - lustreEndShadowWidth / ((1 - 1 / (1 + lST.k * lST.k)).pow(0.5f)), C.y) else Point(S2.x - lustreEndShadowWidth, C.y)
    val T3 = if (!lST.k.isNaN()) Point(C.x, Line.withKAndOnePoint(lST.k, S3).y(C.x)) else Point(S3.x, O.y)
    val lS2T2 = Line.withKAndOnePoint(lST.k, S2)
    val lS3T3 = Line.withKAndOnePoint(lST.k, S3)
    val lS4T4 = Line.withKAndOnePoint(lST.k, S4)

    val lustreEndShadow = Path().apply {
        if (lustreEndDistance + lustreEndShadowWidth > H.distanceTo(lST)) {
            moveTo(O)
            close()
            return@apply
        }
        moveTo(S2)
        lineTo(T2)
        lineTo(T3)
        lineTo(S3)
        close()
        op(this, thisPageBack, operation = PathOperation.Intersect)
    }

    val lustreStartShadow = Path().apply {
        if (lustreEndDistance + lustreEndShadowWidth > H.distanceTo(lST)) {
            moveTo(O)
            close()
            return@apply
        }
        moveTo(S)
        lineTo(T)
        lineTo(T4)
        lineTo(S4)
        close()
        op(this, thisPageBack, operation = PathOperation.Intersect)
    }

    //求阴影控制点
    val lKL = Line.withKAndOnePoint(lST.k, W)
    val lS1T1 = Line.withKAndOnePoint(lST.k, S1)
    val lHC = Line.withTwoPoints(H, C)

    return PathResult(
        listOf(thisPage, thisPageBack, nextPage),
        listOf(shadow1, shadow2, shadow3, shadow4, lustreEndShadow, lustreStartShadow),
        listOf(
            Pair(H.avoidNaN(), H2.avoidNaN()),
            Pair(H.avoidNaN(), H1.avoidNaN()),
            Pair(lKL.intersectAt(lHC).avoidNaN(), lS1T1.intersectAt(lHC).avoidNaN()),
            Pair(lS2T2.intersectAt(lHC).avoidNaN(), lS3T3.intersectAt(lHC).avoidNaN()),
            Pair(lST.intersectAt(lHC).avoidNaN(), lS4T4.intersectAt(lHC).avoidNaN())
        ),
        shadow12Width
    )
}

/**
 * 获取Bitmap扭曲后的格点，相对坐标系
 * @return 第一个数组表示扭曲后边界格点[NVJ, ZTN, WSM, MUI]，第二个数组表示扭曲后格点，第三个数组表示扭曲格子数[宽，高]
 */
private fun AllPoints.buildDistortionPoints(bitmap: Bitmap, absO: Point, isUpsideDown: Boolean): Triple<Array<List<Float>>, FloatArray, Array<Int>> {
    val meshWidthCount = bitmap.width / distortionInterval
    val meshHeightCount = bitmap.height / distortionInterval
    val size = (meshWidthCount + 1) * (meshHeightCount + 1) * 2
    val vertices = FloatArray(size)
    val originalVertices = FloatArray(size)
    val height = bitmap.height.toFloat()
    val width = bitmap.width.toFloat()

    val kCH = Line.withTwoPoints(C, H).k
    val lWZ = Line.withKAndOnePoint(-1 / kCH, W)
    val Jr = Point(C.x, J.y + kCH * (C.x - J.x))
    val lIrJr = Line.withKAndOnePoint(lWZ.k, Jr)
    val Ir = Point(lIrJr.x(C.y), C.y)
    val lJH = Line.withTwoPoints(J, H)
    val lIH = Line.withTwoPoints(I, H)
    val F = Point(C.x, lJH.y(C.x))
    val E = Point(lIH.x(C.y), C.y)
    val delta = N.distanceTo(lWZ) * PI.toFloat() / J.distanceTo(Jr)
    val r = N.distanceTo(lWZ)
    val absC = absO.absScreenSystemWith(C)
    val absH = absO.absScreenSystemWith(H)

    var index = 0
    (0..meshHeightCount).forEach { i ->
        val y = height * i / meshHeightCount
        (0..meshWidthCount).forEach { j ->
            val x = width * j / meshWidthCount
            //vertices格式为[x1,y1] [x2,y2] [x3,y3]
            val xIndex = index * 2
            vertices[xIndex] = x
            vertices[xIndex + 1] = y
            originalVertices[xIndex] = x
            originalVertices[xIndex + 1]
            index++
        }
    }

    val ZTNPoints = mutableListOf<Float>()
    val NVJPoints = mutableListOf<Float>()
    val WSMPoints = mutableListOf<Float>()
    val MUIPoints = mutableListOf<Float>()
    val NVJRange = FloatRange(Jr.y, F.y)
    val ZTNRange = FloatRange(F.y, Z.y)
    val WSMRange = FloatRange(E.x, W.x)
    val MUIRange = FloatRange(Ir.x, E.x)

    val bottomMinXIndex = (meshWidthCount + 1) * meshHeightCount * 2

    (0 until size / 2).forEach { i ->
        val xIndex = i + i
        val yIndex = xIndex + 1
        val x = vertices[xIndex]
        val y = vertices[yIndex]

        val absG = Point(x, y)
        val G = Point(x, y).inCoordinateSystem(absO)

        if (!isUpsideDown) {
            if (!G.isBelow(lWZ)) return@forEach
        } else {
            if (!G.isAbove(lWZ)) return@forEach
        }

        val d = G.distanceTo(lWZ)
        val absQ = absG.getExtensionPointInAbs(Line.withKAndOnePoint(Line.withTwoPoints(absC, absH).k, absG), d - r * sin(d * delta / r))

        //如果是最右侧边界点
        if ((xIndex / 2 + 1) % (meshWidthCount + 1) == 0) {
            when {
                NVJRange.contains(G.y) -> {
                    NVJPoints.apply {
                        add(absQ.x)
                        add(absQ.y)
                    }
                }
                ZTNRange.contains(G.y) -> {
                    ZTNPoints.apply {
                        add(absQ.x)
                        add(absQ.y)
                    }
                }
            }
        }
        //如果是最下（上）侧边界点
        else if (if (!isUpsideDown) xIndex >= bottomMinXIndex else xIndex <= 2 * meshWidthCount) {
            when {
                WSMRange.contains(G.x) -> {
                    WSMPoints.apply {
                        add(absQ.x)
                        add(absQ.y)
                    }
                }
                MUIRange.contains(G.x) -> {
                    MUIPoints.apply {
                        add(absQ.x)
                        add(absQ.y)
                    }
                }
            }
        }

        vertices[xIndex] = absQ.x
        vertices[yIndex] = absQ.y
    }

    return Triple(arrayOf(NVJPoints, ZTNPoints, WSMPoints, MUIPoints), vertices, arrayOf(meshWidthCount, meshHeightCount))
}

/**
 * 绝对系下处理手势翻转
 * @param line 屏幕中垂线
 */
private fun DragEvent.getSymmetricalDragEventAbout(line: Line): DragEvent {
    val origin = originTouchPoint.getSymmetricalPointAbout(line)
    val current = currentTouchPoint.getSymmetricalPointAbout(line)
    return copy(originTouchPoint = origin, currentTouchPoint = current)
}

private data class PathResult(val pagePaths: List<Path>, val shadowPaths: List<Path>, val shaderControlPointPairs: List<Pair<Point, Point>>, val shadow12Width: Float)

private data class DragEvent(val originTouchPoint: Point, val currentTouchPoint: Point) {
    val dragDelta get() = currentTouchPoint - originTouchPoint

    fun inCoordinateSystem(absO: Point): DragEvent {
        val newOrigin = originTouchPoint.inCoordinateSystem(absO)
        val newCur = currentTouchPoint.inCoordinateSystem(absO)
        return DragEvent(newOrigin, newCur)
    }

    val isUnmoved get() = currentTouchPoint.x == originTouchPoint.x && currentTouchPoint.y == originTouchPoint.y

    fun directionToLineInCoordinateSystem(line: Line): DragDirection = when {
        line.k == 0f -> directionToOInCoordinateSystem()
        line.k > 0f -> {
            val lParallel = Line.withKAndOnePoint(line.k, originTouchPoint)
            val lVertical = Line.withKAndOnePoint(-1 / line.k, originTouchPoint)
            currentTouchPoint.run {
                when {
                    isOn(lParallel) && isAbove(lVertical) -> DragDirection.right
                    isAbove(lParallel) && isAbove(lVertical) -> DragDirection.rightUp
                    isAbove(lParallel) && isOn(lVertical) -> DragDirection.up
                    isAbove(lParallel) && isBelow(lVertical) -> DragDirection.leftUp
                    isOn(lParallel) && isBelow(lVertical) -> DragDirection.left
                    isBelow(lParallel) && isBelow(lVertical) -> DragDirection.leftDown
                    isBelow(lParallel) && isOn(lVertical) -> DragDirection.down
                    isBelow(lParallel) && isAbove(lVertical) -> DragDirection.rightDown
                    else -> DragDirection.static
                }
            }
        }
        line.k < 0f -> {
            val lParallel = Line.withKAndOnePoint(line.k, originTouchPoint)
            val lVertical = Line.withKAndOnePoint(-1 / line.k, originTouchPoint)
            currentTouchPoint.run {
                when {
                    isOn(lParallel) && isBelow(lVertical) -> DragDirection.right
                    isAbove(lParallel) && isBelow(lVertical) -> DragDirection.rightUp
                    isAbove(lParallel) && isOn(lVertical) -> DragDirection.up
                    isAbove(lParallel) && isAbove(lVertical) -> DragDirection.leftUp
                    isOn(lParallel) && isAbove(lVertical) -> DragDirection.left
                    isBelow(lParallel) && isAbove(lVertical) -> DragDirection.leftDown
                    isBelow(lParallel) && isOn(lVertical) -> DragDirection.down
                    isBelow(lParallel) && isBelow(lVertical) -> DragDirection.rightDown
                    else -> DragDirection.static
                }
            }
        }
        else -> DragDirection.static
    }

    fun directionToOInCoordinateSystem(): DragDirection {
        return dragDelta.run {
            when {
                x > 0f && y == 0f -> DragDirection.right
                x > 0f && y > 0f -> DragDirection.rightUp
                x == 0f && y > 0f -> DragDirection.up
                x < 0f && y > 0f -> DragDirection.leftUp
                x < 0f && y == 0f -> DragDirection.left
                x < 0f && y < 0f -> DragDirection.leftDown
                x == 0f && y < 0f -> DragDirection.down
                x > 0f && y < 0f -> DragDirection.rightDown
                else -> DragDirection.static
            }
        }
    }
}

private enum class DragDirection {
    right, rightUp, up, leftUp, left, leftDown, down, rightDown, static
}

private fun DragDirection.isIn(vararg directions: DragDirection) = directions.contains(this)

data class Point(var x: Float, var y: Float) {
    operator fun minus(a: Point) = Point(x - a.x, y - a.y)

    operator fun plus(a: Point) = Point(x + a.x, y + a.y)

    operator fun times(m: Float) = Point(x * m, y * m)

    operator fun rangeTo(a: Point) = Point(0.5f * (x + a.x), 0.5f * (y + a.y))

    /**
     * 将相对系坐标转换为绝对系坐标 注意：此this为绝对原点
     * @param absX 要转换的点
     * @param this 绝对原点
     * @return 绝对系坐标
     */
    internal fun absScreenSystemWith(absX: Point) = Point(absX.x + x, -absX.y + y)

    /**
     * 将绝对系坐标转换为相对系坐标，y与系统y坐标相反，且原点为相对原点
     * @param absO 绝对原点
     * @return 相对系坐标
     */
    internal fun inCoordinateSystem(absO: Point) = (this - absO).reverseY

    internal fun getKWith(a: Point) = (y - a.y) / (x - a.x)
}

private fun Point.getSymmetricalPointAbout(line: Line) = with(line) {
    if (k == 0f) {
        Point(x, 2 * b - y)
    } else {
        Point(x - 2 * k * (k * x - y + b) / (k * k + 1), y + 2 * (k * x - y + b) / (k * k + 1))
    }
}

private fun Point.distanceTo(anotherPoint: Point) = sqrt((x - anotherPoint.x).pow(2) + (y - anotherPoint.y).pow(2))

private fun Point.distanceTo(line: Line) = (line.k * x - y + line.b).absoluteValue / (1 + line.k.pow(2)).pow(0.5f)

private fun Point.isBelow(line: Line) = line.k * x + line.b > y

private fun Point.isAbove(line: Line) = line.k * x + line.b < y

private fun Point.isOn(line: Line) = line.k * x + line.b == y

private inline val Point.reverseY
    get() = this.apply {
        y = -y
    }

private val Offset.toPoint get() = Point(x, y)
private val Point.toOffset get() = Offset(x, y)

private fun Path.moveTo(p: Point?) = p?.let { this.moveTo(p.x, p.y) }
private fun Path.lineTo(p: Point?) = p?.let { this.lineTo(p.x, p.y) }
private fun Path.quadraticBezierTo(controlPoint: Point?, endPoint: Point?) {
    if (controlPoint != null && endPoint != null) {
        this.quadraticBezierTo(controlPoint.x, controlPoint.y, endPoint.x, endPoint.y)
    }
}

private fun Path.connect(points: List<Float>, reverse: Boolean = false) {
    val size = points.size / 2
    (0 until size).forEach { i ->
        val xIndex = i + i
        if (!reverse) {
            lineTo(points[xIndex], points[xIndex + 1])
        } else {
            val rXIndex = size + size - xIndex - 2
            lineTo(points[rXIndex], points[rXIndex + 1])
        }
    }
}

//如果有NaN就置0
private fun Point.avoidNaN() = if (x.isNaN() || y.isNaN()) Point(0f, 0f) else this

/**
 * y = kx + b
 */
private data class Line(val k: Float, val b: Float) {
    fun intersectAt(other: Line) = Point(0f, 0f).apply {
        x = (other.b - b) / (k - other.k)
        y = k * x + b
    }

    fun y(x: Float) = k * x + b

    fun x(y: Float) = (y - b) / k

    //相对系
    fun theta() = theta(k)

    companion object {
        //两点式
        fun withTwoPoints(m: Point, n: Point): Line {
            val k = (n.y - m.y) / (n.x - m.x)
            val b = m.y - k * m.x
            return Line(k, b)
        }

        //点斜式
        fun withKAndOnePoint(k: Float, p: Point): Line {
            return Line(k, p.y - k * p.x)
        }

        //斜率=>倾斜角（弧度制）
        fun theta(k: Float) = atan(k).let { if (it < 0) PI.toFloat() + it else it }

        //倾斜角=>斜率（弧度制）
        fun k(theta: Float) = tan(theta)
    }
}

//ax²+bx+c=0
private data class QuadraticEquationWithOneUnknown(val a: Float, val b: Float, val c: Float) {
    fun solve(): Array<Float> {
        val delta = b * b - 4 * a * c
        return if (delta < 0) emptyArray() else arrayOf((-b + sqrt(delta)) / (2 * a), (-b - sqrt(delta)) / (2 * a))
    }
}

private fun Float.toRad() = ((this * PI) / 180).toFloat()
private fun Float.toDeg() = (this * 180 / PI).toFloat()

//获取一个点在同一直线方向上延伸d的另一个点，d大于0则向左(若line的斜率不存在，则向下)延伸，屏幕绝对坐标系
private fun Point.getExtensionPointInAbs(line: Line, d: Float): Point {
    if (line.k.isNaN()) return Point(x, y + d)

    val newX = x - d / (1 + line.k * line.k).pow(0.5f)
    return Point(newX, line.y(newX))
}

//角平分线
private fun getAngularBisector(l1: Line, l2: Line, crossPoint: Point = l1.intersectAt(l2)): Pair<Line, Line> {
    //偷个懒。
    if (l1.k.isNaN() || l2.k.isNaN()) return Pair(Line.withTwoPoints(Point(0f, 0f), Point(0f, 0f)), Line.withTwoPoints(Point(0f, 0f), Point(0f, 0f)))

    val u = (1 + l1.k * l1.k).pow(0.5f)
    val v = (1 + l2.k * l2.k).pow(0.5f)

    val k1 = (l2.k * v + l1.k * u) / (u + v)
    val k2 = -1 / k1
    return Pair(Line.withKAndOnePoint(k1, crossPoint), Line.withKAndOnePoint(k2, crossPoint))
}

private data class FloatRange(val start: Float, val end: Float) {
    fun constraints(value: Float) = when {
        end in start..value -> end
        start in value..end -> start
        end in value..start -> end
        start in end..value -> start
        else -> value
    }

    fun linearMapping(nowValue: Float, newRange: FloatRange): Float {
        val min2 = newRange.start
        val max2 = newRange.end
        return min2 + (max2 - min2) * (nowValue - start) / (end - start)
    }

    fun linearMappingWithConstraints(nowValue: Float, newRange: FloatRange): Float {
        return when {
            end in start..nowValue -> newRange.end
            start in nowValue..end -> newRange.start
            end in nowValue..start -> newRange.end
            start in end..nowValue -> newRange.start
            else -> linearMapping(nowValue, newRange)
        }
    }

    fun contains(value: Float) = value in start..end || value in end..start
}

private data class AllPoints(
    val O: Point,
    val A: Point,
    val B: Point,
    val C: Point,
    val H: Point,
    val I: Point,
    val J: Point,
    val M: Point,
    val N: Point,
    val S: Point,
    val T: Point,
    val U: Point,
    val V: Point,
    val W: Point,
    val Z: Point
) {
    fun toAbsSystem(absO: Point) = with(absO) {
        AllPoints(
            absScreenSystemWith(O),
            absScreenSystemWith(A),
            absScreenSystemWith(B),
            absScreenSystemWith(C),
            absScreenSystemWith(H),
            absScreenSystemWith(I),
            absScreenSystemWith(J),
            absScreenSystemWith(M),
            absScreenSystemWith(N),
            absScreenSystemWith(S),
            absScreenSystemWith(T),
            absScreenSystemWith(U),
            absScreenSystemWith(V),
            absScreenSystemWith(W),
            absScreenSystemWith(Z)
        )
    }

    companion object {
        fun default(absO: Point) = AllPoints(
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy(),
            absO.copy()
        )
    }

    fun toCoordinateSystem(absO: Point) = with(absO) {
        AllPoints(
            O.inCoordinateSystem(this),
            A.inCoordinateSystem(this),
            B.inCoordinateSystem(this),
            C.inCoordinateSystem(this),
            H.inCoordinateSystem(this),
            I.inCoordinateSystem(this),
            J.inCoordinateSystem(this),
            M.inCoordinateSystem(this),
            N.inCoordinateSystem(this),
            S.inCoordinateSystem(this),
            T.inCoordinateSystem(this),
            U.inCoordinateSystem(this),
            V.inCoordinateSystem(this),
            W.inCoordinateSystem(this),
            Z.inCoordinateSystem(this)
        )
    }

    fun getSymmetricalPointAboutLine(line: Line) = with(line) {
        AllPoints(
            O.getSymmetricalPointAbout(this),
            A.getSymmetricalPointAbout(this),
            B.getSymmetricalPointAbout(this),
            C.getSymmetricalPointAbout(this),
            H.getSymmetricalPointAbout(this),
            I.getSymmetricalPointAbout(this),
            J.getSymmetricalPointAbout(this),
            M.getSymmetricalPointAbout(this),
            N.getSymmetricalPointAbout(this),
            S.getSymmetricalPointAbout(this),
            T.getSymmetricalPointAbout(this),
            U.getSymmetricalPointAbout(this),
            V.getSymmetricalPointAbout(this),
            W.getSymmetricalPointAbout(this),
            Z.getSymmetricalPointAbout(this)
        )
    }
}
