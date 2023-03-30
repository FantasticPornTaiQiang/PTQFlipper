package ptq.mpga.pinance.widget

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ptq.mpga.pinance.widget.Line.Companion.k
import ptq.mpga.pinance.widget.Line.Companion.theta
import kotlin.math.*

private const val TAG = "PTQPageFlipper"

//带ratio的要乘屏幕比例
private const val stateTightMinWERatio = 1 / 8f //进入tight状态的最小WE长度
private const val minTheta = ((137f * PI) / 180).toFloat() //最小θ，弧度制
private const val stateTightDragUpThetaRate = 0.1f //tight状态向上拉扯的速率，该值越大则拉扯越快
private const val stateTightDragRightScreenDistanceRatio = 1 / 40f //tight状态手指移到屏幕右侧多少时到达最终态
private const val maxDragXRatio = stateTightDragRightScreenDistanceRatio //手指最大X，和上个变量一样
private const val minWxRatio = 1 / 25f //W.x的最小值
private const val WKtoKSMax = 1f //loose状态WK:KS的最大值

private const val animStartTimeout = 100 //动画在手指拖动多久后开始，此值不宜过大，会造成不准确
private const val animEnterDuration = 100 //入动画时间，此值不宜过大，会造成不准确
private const val animExitDuration = animEnterDuration //出动画时间

private const val tapYDeltaRatio = 1 / 170f //点击翻页时，y方向偏移，不宜过大

private const val shadowThreshold = 35f //阴影1、2阈值
private const val shadowPart3to1Ratio = 1.5f //阴影3与阴影1的宽度比
private const val shadow3VeticalThreshold = 50 //处理当接近垂直时，底层绘制api不正常工作的问题

private enum class PageState {
    loose, wMin, thetaMin, tight
}

private enum class State {
    idle, enterAnimStart, draggable, exitAnimStart, exitAnimPreStart
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PTQPageFlipper(modifier: Modifier = Modifier) {
    //组件宽高和原点O
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val dragXRange = FloatRange(0f, screenWidth * (1 - maxDragXRatio))

    val absO = Point(0f, 0f)
    //位于组件一半高度的直线，用于处理翻转，绝对坐标系
    val lBCPerpendicularBisector = Line(0f, screenHeight / 2)

    //拖动事件，包括原触点，增量，现触点
    var curDragEvent by remember { mutableStateOf(DragEvent(absO.copy(), absO.copy())) }
    //当前状态
    var pageState by remember { mutableStateOf(PageState.loose) }
    //要绘制的所有点，绝对系
    var allPoints by remember { mutableStateOf(AllPoints.default(absO)) }
    //当前翻转状态
    var upsideDown by remember { mutableStateOf(false) }

    //倾角θ
    var theta by remember { mutableStateOf(0f) }
    //fixedValue
    var f by remember { mutableStateOf(0f) }

    //流程状态
    var state: State by remember { mutableStateOf(State.idle) }
    //入动画还未结束就松手了
    var exitPreStartState by remember { mutableStateOf(false) }
    //点击翻页则匀一下进出时间
    var animDuration by remember { mutableStateOf(arrayOf(animEnterDuration, animExitDuration)) }

    //动画的初始和最终状态
    var animStartAndFinalPoint by remember { mutableStateOf(DragEvent(absO.copy(), absO.copy())) }
    //动画的上一个点origin
    var animLastPoint by remember { mutableStateOf(absO.copy()) }
    //定时，拖动多久后开始播放动画
    var time by remember { mutableStateOf(0L) }

    val exeExitAnim = {
        val startPoint = if (!upsideDown) allPoints.J else allPoints.J.getSymmetricalPointAbout(lBCPerpendicularBisector)
        f = maxOf(0f, allPoints.J.distanceTo(allPoints.H))
        val y = if (!upsideDown) screenHeight - f else f
        animStartAndFinalPoint = DragEvent(startPoint.copy(), if (curDragEvent.currentTouchPoint.x > 0.5f * screenWidth) Point(dragXRange.end, y) else Point(-dragXRange.end / 2, y))
        animLastPoint = startPoint.copy()
        pageState = PageState.loose
        state = State.exitAnimStart
    }

    //动画
    val animFloatRatio by animateFloatAsState(targetValue = if (state == State.enterAnimStart || state == State.exitAnimStart) 1f else 0f, finishedListener = {
        //动画结束时
        when (state) {
            State.enterAnimStart -> {
                state = if (exitPreStartState) State.exitAnimPreStart else State.draggable
            }
            State.exitAnimStart -> {
                curDragEvent = DragEvent(absO.copy(), absO.copy())
                state = State.idle
                exitPreStartState = false
                allPoints = AllPoints.default(absO)
                upsideDown = false
                pageState = PageState.loose
                time = 0L
                theta = 0f
                f = 0f
                animLastPoint = absO.copy()
                animStartAndFinalPoint = DragEvent(absO.copy(), absO.copy())
                animDuration = arrayOf(animEnterDuration, animExitDuration)
            }
            State.exitAnimPreStart -> { //确保animFloatRatio回到0f
                exeExitAnim()
            }
            else -> {}
        }
    }, animationSpec = TweenSpec(easing = LinearEasing, durationMillis = if (state == State.enterAnimStart) animDuration[0] else if (state == State.exitAnimStart) animDuration[1] else 0))

    val onDragEnd = {
        if (state == State.draggable) {
            exeExitAnim()
        } else if (state == State.enterAnimStart) {
            exitPreStartState = true
        }
    }

    Canvas(modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { offset: Offset ->
                if (state == State.idle && dragXRange.contains(offset.x)) {
                    val touchPoint = offset.toPoint
                    val tapYDelta = tapYDeltaRatio * screenHeight
                    if (touchPoint.y > tapYDelta) {
                        val isRightToLeft = touchPoint.x < 0.5f * screenWidth
                        val startPoint = Point(x = if (isRightToLeft) -dragXRange.end * 0.5f else dragXRange.end, y = touchPoint.y - tapYDelta)
                        f = screenHeight - touchPoint.y.absoluteValue
                        val endPoint = Point(x = if (isRightToLeft) dragXRange.end else -dragXRange.end * 0.5f, y = touchPoint.y)
                        animLastPoint = startPoint.copy()
                        animStartAndFinalPoint = DragEvent(startPoint.copy(), endPoint.copy())
                        pageState = PageState.loose
                        animDuration = arrayOf(animEnterDuration, animEnterDuration + animExitDuration)
                        state = State.exitAnimStart
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    if (state == State.idle && dragXRange.contains(offset.x)) {
                        val touchPoint = offset.toPoint
                        pageState = PageState.loose
                        curDragEvent = DragEvent(touchPoint, touchPoint)
                        f = screenHeight - touchPoint.y.absoluteValue
                        animStartAndFinalPoint = animStartAndFinalPoint.copy(originTouchPoint = touchPoint)
                        time = System.currentTimeMillis()
                    }
                },
                onDrag = { _, dragAmount ->
                    val cur = (curDragEvent.currentTouchPoint + dragAmount.toPoint)/*.getXConstraintTouchPoint(screenWidth)*/
                    curDragEvent = DragEvent(curDragEvent.currentTouchPoint.copy(), cur)

                    if (!dragXRange.contains(cur.x)) {
                        onDragEnd()
                        return@detectDragGestures
                    }

                    if (state == State.idle) {
                        if (System.currentTimeMillis() - time > animStartTimeout) {
                            if (animStartAndFinalPoint
                                    .directionToOInCoordinateSystem()
                                    .isIn(DragDirection.up, DragDirection.down, DragDirection.static)
                            ) return@detectDragGestures

                            if (!dragXRange.contains(animStartAndFinalPoint.originTouchPoint.x))
                                return@detectDragGestures

                            //手指从右向左翻，还是从左向右
                            animStartAndFinalPoint = animStartAndFinalPoint.copy(currentTouchPoint = cur)
                            val isRightToLeft = animStartAndFinalPoint.currentTouchPoint.x < animStartAndFinalPoint.originTouchPoint.x
                            animStartAndFinalPoint.originTouchPoint.x = if (isRightToLeft) (dragXRange.end) else (dragXRange.start)
                            //如果向下翻，默认向上
                            val isUpToDown = animStartAndFinalPoint.currentTouchPoint.y > animStartAndFinalPoint.originTouchPoint.y
                            if (isUpToDown) {
                                upsideDown = true
                                f = screenHeight - f
                                Log.d(TAG, "PTQPageFlipper:  upsdiedown1 $upsideDown")
                            }
                            animLastPoint = animStartAndFinalPoint.originTouchPoint.copy()
                            state = State.enterAnimStart
                        }
                    }

                    if (state == State.draggable) {
                        val dragEvent = if (upsideDown) curDragEvent.getSymmetricalDragEventAbout(lBCPerpendicularBisector) else curDragEvent

                        if (pageState == PageState.tight) {
                            val newTheta = onDragWhenTightState(absO.copy(), f, theta, dragEvent, screenHeight, screenWidth)
                            theta = newTheta
                        }
                    }
                },
                onDragEnd = {
                    onDragEnd()
                }
            )
        }
    ) {
        //如果正在动画，则使用动画的animDragEvent，否则使用拖动的
        var dragEvent = when(state) {
            State.enterAnimStart, State.exitAnimStart -> {
                val curPoint = animStartAndFinalPoint.originTouchPoint + animStartAndFinalPoint.dragDelta * animFloatRatio
                val animDragEvent = DragEvent(animLastPoint, curPoint)
                animLastPoint = curPoint
                animDragEvent
            }
            State.draggable -> {
                curDragEvent
            }
            else -> {
                null
            }
        }

        //计算本轮点坐标，如果有状态变化，则舍弃本轮
        val newAllPoints = if (dragEvent != null && !dragEvent.isUnmoved) {
            dragEvent = if (upsideDown) dragEvent.getSymmetricalDragEventAbout(lBCPerpendicularBisector) else dragEvent

            when (pageState) {
                PageState.loose -> {
                    buildStateLoose(absO.copy(), dragEvent, Point(screenWidth, screenHeight), f, upsideDown, state) { newState, newTheta, newEmaxInThetaMin, newUpsideDown ->
                        newUpsideDown?.let {
                            Log.d(TAG, "PTQPageFlipper:  upsdiedown2 $it")
                            upsideDown = it
                            f = screenHeight - f
                            return@buildStateLoose
                        }
                        newState?.let { pageState = it }
                        newTheta?.let { theta = it }
//                        newEmaxInThetaMin?.let { EmaxInMinTheta = it }
                        Log.d(TAG, "PTQPageFlipper: state $pageState start")
                    }
                }
                PageState.wMin -> {
                    buildStateWMin(absO.copy(), dragEvent, Point(screenWidth, screenHeight), f) { newState, newTheta ->
                        theta = newTheta
                        pageState = newState
                        if (newState == PageState.thetaMin) {
//                            EmaxInMinTheta = dragXRange.end
                        }
                        Log.d(TAG, "PTQPageFlipper: state $pageState start")
                    }
                }
                PageState.thetaMin -> {
                    buildStateThetaMin(absO.copy(), dragEvent, Point(screenWidth, screenHeight), f) { newState, newTheta ->
                        theta = newTheta
                        pageState = newState
                        Log.d(TAG, "PTQPageFlipper: state $pageState start")
                    }
                }
                PageState.tight -> {
                    buildStateTight(absO.copy(), Point(screenWidth, screenHeight), theta, dragEvent, f) { newState ->
                        pageState = newState
                        Log.d(TAG, "PTQPageFlipper: state $newState start")
                    }
                }
            }
        } else {
            null
        }

//        val color = { index: Int ->
//            when (index) {
//                0 -> Color.Blue
//                1 -> Color.Green
//                2 -> Color.Yellow
//                else -> Color.Black
//            }
//        }

        val all = newAllPoints?.apply {
            allPoints = this
        } ?: allPoints

        (if (upsideDown) all.getSymmetricalPointAboutLine(lBCPerpendicularBisector) else all).buildPath { paths, shadowPaths, shaderPointPairs ->
            Log.d(TAG, "PTQPageFlipper: $shaderPointPairs")
            val shadow12Color = android.graphics.Color.toArgb(Color.Black.copy(alpha = 0.3f).value.toLong())
            val shadow3Color = android.graphics.Color.toArgb(Color.Black.copy(alpha = 0.6f).value.toLong())
            val transparentColor = android.graphics.Color.toArgb(Color.Transparent.value.toLong())
            val thisColor = android.graphics.Color.toArgb(Color.Magenta.value.toLong())
            val thisBackColor = android.graphics.Color.toArgb(Color.White.value.toLong())
            val nextColor = android.graphics.Color.toArgb(Color.Yellow.value.toLong())
            drawIntoCanvas {
                val paint = Paint()
                val frameworkPaint = paint.asFrameworkPaint()
                //注意图层绘制顺序
                frameworkPaint.color = thisBackColor
                it.drawPath(paths[2], paint)
                frameworkPaint.shader = LinearGradient(shaderPointPairs[2].first.x, shaderPointPairs[2].first.y, shaderPointPairs[2].second.x, shaderPointPairs[2].second.y, shadow3Color, transparentColor, Shader.TileMode.CLAMP)
                it.drawPath(shadowPaths[2], paint)
                frameworkPaint.shader = null
                frameworkPaint.color = thisBackColor
                it.drawPath(paths[0], paint)
                frameworkPaint.shader = LinearGradient(shaderPointPairs[0].first.x, shaderPointPairs[0].first.y, shaderPointPairs[0].second.x, shaderPointPairs[0].second.y, shadow12Color, transparentColor, Shader.TileMode.CLAMP)
                it.drawPath(shadowPaths[0], paint)
                frameworkPaint.shader = LinearGradient(shaderPointPairs[1].first.x, shaderPointPairs[1].first.y, shaderPointPairs[1].second.x, shaderPointPairs[1].second.y, shadow12Color, transparentColor, Shader.TileMode.CLAMP)
                it.drawPath(shadowPaths[1], paint)
                frameworkPaint.shader = null
                frameworkPaint.color = thisBackColor
                it.drawPath(paths[1], paint)
            }
        }
    }
}

private inline fun buildStateLoose(absO: Point, dragEvent: DragEvent, absC: Point, f: Float, upsideDown: Boolean, state: State, changeState: (newPageState: PageState?, theta: Float?, EmaxInThetaMin: Float?, upsideDown: Boolean?) -> Unit): AllPoints? {
    val (theta, points) = algorithmStateLoose(absO, dragEvent.originTouchPoint, absC, f, state == State.exitAnimStart)

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val lHF = Line.withTwoPoints(points.H, dragEventCoordinate.originTouchPoint)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)
    val flipDragDirection = dragEventCoordinate.directionToOInCoordinateSystem()
    val Ex = Line.withKAndOnePoint(-1 / Line.withTwoPoints(points.C, points.H).k, points.C..points.H).x(points.C.y)
    val kJH = Line.withTwoPoints(points.J, points.H).k

//    Log.d(TAG, "buildStateLoose: kJH$kJH  W.x${points.W.x} minWx${minWxRatio * points.C.x} theta${theta.toDeg()} direction${dragDirection}")

    if (dragDirection == DragDirection.static || kJH.isNaN()) return null

    //TODO: 状态转变用curTouchPoint计算

    if (state == State.enterAnimStart || state == State.draggable) {
        when {
            //翻转
            kJH <= 0 && flipDragDirection.isIn(DragDirection.down, DragDirection.rightDown, DragDirection.leftDown, DragDirection.left, DragDirection.right) -> {
                changeState(null, null, null, !upsideDown)
                return null
            }
            points.W.x < minWxRatio * points.C.x && dragDirection.isIn(DragDirection.up, DragDirection.rightUp, DragDirection.leftUp, DragDirection.leftDown) -> {
                changeState(PageState.wMin, theta, null, null)
                return null
            }
            theta < minTheta && dragDirection.isIn(DragDirection.up, DragDirection.rightUp, DragDirection.right) -> {
                changeState(PageState.thetaMin, minTheta, Ex, null)
                return null
            }
        }
    }


    return points.toAbsSystem(absO)
}

//allowLeftOut:是否允许从左侧翻出去，即Wx不受限，用于动画
private fun algorithmStateLoose(absO: Point, absR: Point, absC: Point, f: Float, allowLeftOut: Boolean): Pair<Float, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val A = Point(0f, C.y)
    val B = Point(C.x, 0f)
    val R = absR.inCoordinateSystem(absO)
    val minWx = minWxRatio * C.x

    val Rf = Point(C.x, C.y + f)
    val k = (C.x - R.x) / (R.y - Rf.y)

    val lKL = Line.withKAndOnePoint(k, R)
    val K = Point(lKL.x(C.y), C.y)
    val L = Point(C.x, lKL.y(C.x))

    val lEF = Line.withKAndOnePoint(k, R..Rf)
    val E = Point(lEF.x(C.y), C.y)
    val F = Point(C.x, lEF.y(C.x))

    val S = K..E
    val T = L..F

    val lCH = Line.withKAndOnePoint(-1 / k, C)

    val P = lEF.intersectAt(lCH)
    val H = Point(2 * P.x - C.x, 2 * P.y - C.y)

    val lHE = Line.withTwoPoints(E, H)
    val lHF = Line.withTwoPoints(F, H)
    val lST = Line.withKAndOnePoint(k, S)
    val J = R.copy()
    val I = lHE.intersectAt(lKL)
    val U = lHE.intersectAt(lST)
    val V = lHF.intersectAt(lST)

    val M = U..S
    val N = T..V

    val W = if (!allowLeftOut) {
        val KRange = FloatRange(minWx, C.x)
        val WKRatioRange = FloatRange(0f, WKtoKSMax)
        val thetaRange = FloatRange(minTheta, 180f.toRad())
        val KMappingValue = KRange.linearMappingWithConstraints(K.x, WKRatioRange)
        val thetaMappingValue = thetaRange.linearMappingWithConstraints(lCH.theta(), WKRatioRange)
        val finalMappingValue = KMappingValue * thetaMappingValue
        val Wx = (K.x - finalMappingValue * (E.x - S.x))
        Point(Wx, C.y)
    } else {
        Point(K.x, C.y)
    }

    val lWZ = Line.withKAndOnePoint(lEF.k, W)
    val Z = Point(C.x, lWZ.y(C.x))

    val allPoints = AllPoints(O, A, B, C, H, I, J, K, M, N, S, T, U, V, W, Z)
//    Log.d(TAG, "loose: Kx ${K.x} Wx ${W.x} E ${E.x}")

    return Pair(lCH.theta(), allPoints)
}

private inline fun buildStateWMin(absO: Point, dragEvent: DragEvent, absC: Point, f: Float, changeState: (newPageState: PageState, theta: Float) -> Unit): AllPoints? {
    val (theta, WE, points) = algorithmStateWMin(absO, dragEvent.originTouchPoint, absC, f)
    val (_, stateLoosePoints) = algorithmStateLoose(absO, dragEvent.currentTouchPoint, absC, f, false)
    val minWE = stateTightMinWERatio * points.C.x

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val lHF = Line.withTwoPoints(points.H, dragEventCoordinate.originTouchPoint)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)

    when {
        WE < minWE && dragDirection.isIn(DragDirection.up, DragDirection.rightUp, DragDirection.right, DragDirection.leftUp) -> {
            changeState(PageState.tight, maxOf(minTheta, theta))
            return null
        }
        (stateLoosePoints.W.x >= minWxRatio * points.C.x) && dragDirection.isIn(DragDirection.leftDown, DragDirection.down, DragDirection.rightDown, DragDirection.left) -> {
            changeState(PageState.loose, maxOf(minTheta, theta))
            return null
        }
        //这里有问题，不能直接thetaMin，因为thetaMin的时候根据Wx计算了，但是wMin
        theta < minTheta && dragDirection.isIn(DragDirection.right, DragDirection.up, DragDirection.rightUp, DragDirection.rightDown) -> {
            changeState(PageState.thetaMin, minTheta)
            return null
        }
    }

    return points.toAbsSystem(absO)
}

private fun algorithmStateWMin(absO: Point, absTouchPoint: Point, absC: Point, f: Float): Triple<Float, Float, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val A = Point(0f, C.y)
    val B = Point(C.x, 0f)
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

    val allPoints = AllPoints(O, A, B, C, H, I, J, W, M, N, S, T, U, V, W, Z)
    Log.d(TAG, "wMin: Kx${W.x} Ex${E.x}")

    return Triple(lCH.theta(), E.x - W.x, allPoints)
}

private inline fun buildStateThetaMin(absO: Point, dragEvent: DragEvent, absC: Point, f: Float, changeState: (newPageState: PageState, theta: Float) -> Unit): AllPoints? {
    val (WE, points) = algorithmStateThetaMin(absO, dragEvent.originTouchPoint, absC, f)

    val dragEventCoordinate = dragEvent.inCoordinateSystem(absO)
    val lHF = Line.withTwoPoints(points.H, dragEventCoordinate.originTouchPoint)
    val dragDirection = dragEventCoordinate.directionToLineInCoordinateSystem(lHF)

    val minWx = minWxRatio * points.C.x
    val minWE = stateTightMinWERatio * points.C.x

//    Log.d(TAG, "buildStateThetaMin: W.x${points.W.x} dis:${points.H.distanceTo(dragEventCoordinate.originTouchPoint)} f:${f} dir:${dragDirection}")

    when {
        points.W.x <= minWx && WE <= minWE && dragDirection.isIn(DragDirection.rightUp, DragDirection.leftUp, DragDirection.up, DragDirection.right) -> {
            changeState(PageState.tight, minTheta)
            return null
        }
        points.H.distanceTo(dragEventCoordinate.originTouchPoint) < f && dragDirection.isIn(DragDirection.left, DragDirection.leftDown, DragDirection.rightDown, DragDirection.down, DragDirection.leftUp) -> {
            changeState(PageState.loose, minTheta)
            return null
        }
        points.H.distanceTo(dragEventCoordinate.originTouchPoint) < f && points.W.x <= minWx && dragDirection.isIn(DragDirection.leftDown) -> {
            changeState(PageState.wMin, minTheta)
            return null
        }
    }

    return points.toAbsSystem(absO)
}

private fun algorithmStateThetaMin(absO: Point, absTouchPoint: Point, absC: Point, f: Float): Pair<Float, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val A = Point(0f, C.y)
    val B = Point(C.x, 0f)
    val R = absTouchPoint.inCoordinateSystem(absO)
    val minWx = minWxRatio * C.x
    val minWE = stateTightMinWERatio * C.x

    val lCH = Line.withKAndOnePoint(k(minTheta), C)
    val k = -1 / lCH.k

    val G = Point(C.x, R.y + lCH.k * (C.x - R.x))

    val E = Point(Line.withKAndOnePoint(k, R..G).x(C.y), C.y)
    val F = Point(C.x, k * (C.x - E.x) + C.y)
    val lEF = Line.withKAndOnePoint(k, E)

    val Emax = C.x - f / k(minTheta - PI.toFloat() / 2) - stateTightMinWERatio * C.x

    val ERange = FloatRange(minWE + minWx, maxOf(minWE + minWx, Emax))
    val maxKE = E.x - (R.x - (R.y - C.y) / k)
    val KERange = FloatRange(minWE, maxOf(minWE, maxKE))
    val KE = ERange.linearMappingWithConstraints(E.x, KERange)
    val K = Point(E.x - KE, C.y)
    Log.d(TAG, "algorithmStateThetaMin: ERange:$ERange KERange:$KERange Kx${K.x} Ex${E.x}")
    val lKL = Line.withKAndOnePoint(k, K)

    val L = Point(C.x, lKL.y(C.x))

    val S = K..E
    val T = L..F

    val P = lEF.intersectAt(lCH)
    val H = Point(2 * P.x - C.x, 2 * P.y - C.y)

    val lHE = Line.withTwoPoints(E, H)
    val lHF = Line.withTwoPoints(F, H)
    val lST = Line.withKAndOnePoint(k, S)
    val J = lHF.intersectAt(lKL)
    val I = lHE.intersectAt(lKL)
    val U = lHE.intersectAt(lST)
    val V = lHF.intersectAt(lST)

    val M = U..S
    val N = T..V

    val KRange = FloatRange(minWx, C.x)
    val WKRatioRange = FloatRange(0f, WKtoKSMax)
    val Wx = (K.x - KRange.linearMapping(K.x, WKRatioRange) * (E.x - S.x)).run {
        maxOf(this, minWx)
    }
    val W = Point(Wx, C.y)

    val lWZ = Line.withKAndOnePoint(lEF.k, W)
    val Z = Point(C.x, lWZ.y(C.x))

    val allPoints = AllPoints(O, A, B, C, H, I, J, K, M, N, S, T, U, V, W, Z)

//    Log.d(TAG, "thetaMin: k$K E$E")

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
            changeState(PageState.thetaMin)
            null
        } else {
            changeState(PageState.wMin)
            null
        }
    }

    return allPoints.toAbsSystem(absO)
}

private fun algorithmStateTight(absO: Point, absC: Point, theta: Float): Pair<Line, AllPoints> {
    val O = absO.inCoordinateSystem(absO)
    val C = absC.inCoordinateSystem(absO)
    val B = Point(C.x, 0f)
    val A = Point(0f, C.y)

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

    val allPoints = AllPoints(O, A, B, C, H, I, J, W, M, N, S, T, U, V, W, Z)

    return Pair(lFH, allPoints)
}

//绝对系，根据点计算路径，返回值第一项为页面路径，第二项为阴影路径，第三项为阴影控制点
private fun AllPoints.buildPath(result: (List<Path>, List<Path>, List<Pair<Point, Point>>) -> Unit) {
    val thisPage = Path().apply {
        moveTo(O)
        lineTo(A)
        lineTo(W)
        quadraticBezierTo(S, M)
        quadraticBezierTo(U, I)
        lineTo(H)
        lineTo(J)
        quadraticBezierTo(V, N)
        quadraticBezierTo(T, Z)
        lineTo(B)
        close()
    }

    val thisPageBack = Path().apply {
        moveTo(H)
        lineTo(I)
        quadraticBezierTo(U, M)
        lineTo(N)
        quadraticBezierTo(V, J)
        close()
    }

    val nextPage = Path().apply {
        moveTo(C)
        lineTo(W)
        quadraticBezierTo(S, M)
        lineTo(N)
        quadraticBezierTo(T, Z)
        close()
    }

    val lHF = Line.withTwoPoints(J, H)
    val lHE = Line.withTwoPoints(H, I)
    val KE = (S.x - K.x) * 2
    val KERange = FloatRange(0f, stateTightMinWERatio * (C.x - O.x))
    val shadow12Range = FloatRange(0f, shadowThreshold)
    val shadow12Width = KERange.linearMapping(KE, shadow12Range)
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

    val kST = Line.withTwoPoints(S, T).k
    val S1 = if (!kST.isNaN()) Point(S.x + shadow3Width / ((1 - 1 / (1 + kST * kST)).pow(0.5f)), C.y) else Point(S.x + shadow3Width, C.y)
    val T1 = if (!kST.isNaN()) Point(C.x, Line.withKAndOnePoint(kST, S1).y(C.x)) else Point(S1.x, O.y)

    Log.d(TAG, "buildPath: $H ${lHF.theta().toDeg()}")
    val shadow1 = Path().apply {
        moveTo(H1)
        //处理翻转
        if (C.y == 0f) {
            arcTo(Rect(H.toOffset, shadow12Width), lHF.theta().toDeg() + 180f, -90f, true)
        } else {
            arcTo(Rect(H.toOffset, shadow12Width), lHF.theta().toDeg(), 90f, true)
        }
        lineTo(J1)
        quadraticBezierTo(V1, N)
        quadraticBezierTo(V, J)
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
    }

    val shadow3 = Path().apply {
        reset()
        moveTo(W)
        lineTo(S1)
        //若接近垂直，则直接画成方形，否则画梯形
        if (((T1.y - O.y) / (C.y - O.y)).absoluteValue > shadow3VeticalThreshold) {
            lineTo(S1.copy(y = (C.y - O.y).absoluteValue - S1.y))
            lineTo(W.copy(y = (C.y - O.y).absoluteValue - W.y))
        } else {
            lineTo(T1)
            lineTo(Z)
        }
        close()
    }

    val lKL = Line.withKAndOnePoint(kST, K)
    val lS1T1 = Line.withKAndOnePoint(kST, S1)
    val lHC = Line.withTwoPoints(H, C)

    result(listOf(thisPage, thisPageBack, nextPage), listOf(shadow1, shadow2, shadow3), listOf(Pair(H.processNaN(), H2.processNaN()), Pair(H.processNaN(), H1.processNaN()), Pair(lKL.intersectAt(lHC).processNaN(), lS1T1.intersectAt(lHC).processNaN())))
}

//处理翻转，绝对系
private fun DragEvent.getSymmetricalDragEventAbout(line: Line): DragEvent {
    val origin = originTouchPoint.getSymmetricalPointAbout(line)
    val current = currentTouchPoint.getSymmetricalPointAbout(line)
    return copy(originTouchPoint = origin, currentTouchPoint = current)
}

//如果有NaN就置0
private fun Point.processNaN() = if (x.isNaN() || y.isNaN()) Point(0f, 0f) else this

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

private data class Point(var x: Float, var y: Float) {
    operator fun minus(a: Point) = Point(x - a.x, y - a.y)

    operator fun plus(a: Point) = Point(x + a.x, y + a.y)

    operator fun times(m: Float) = Point(x * m, y * m)

    operator fun rangeTo(a: Point) = Point(0.5f * (x + a.x), 0.5f * (y + a.y))

    fun absScreenSystemWith(absX: Point) = Point(absX.x + x, -absX.y + y)

    /**
     * 坐标系坐标，y与系统y坐标相反，且原点为相对原点
     */
    fun inCoordinateSystem(absO: Point) = (this - absO).reverseY

    fun getKWith(a: Point) = (y - a.y) / (x - a.x)

    fun getSymmetricalPointAbout(line: Line) = with(line) {
        if (k == 0f) {
            Point(x, 2 * b - y)
        } else {
            Point(x - 2 * k * (k * x - y + b) / (k * k + 1), y + 2 * (k * x - y + b) / (k * k + 1))
        }
    }

    fun distanceTo(anotherPoint: Point) = sqrt((x - anotherPoint.x).pow(2) + (y - anotherPoint.y).pow(2))

    fun distanceTo(line: Line) = (line.k * x - y + line.b).absoluteValue / (1 + line.k.pow(2)).pow(0.5f)

    fun isBelow(line: Line) = line.k * x + line.b > y

    fun isAbove(line: Line) = line.k * x + line.b < y

    fun isOn(line: Line) = line.k * x + line.b == y
}

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

private fun getAngularBisector(l1:Line, l2:Line, crossPoint: Point = l1.intersectAt(l2)): Pair<Line, Line> {
    //偷个懒。
    if (l1.k.isNaN() || l2.k.isNaN()) return Pair(Line.withTwoPoints(Point(0f, 0f), Point(0f, 0f)), Line.withTwoPoints(Point(0f, 0f), Point(0f, 0f)))

    val u = (1 + l1.k * l1.k).pow(0.5f)
    val v = (1 + l2.k * l2.k).pow(0.5f)

    val k1 = (l2.k * v + l1.k * u) / (u + v)
    val k2 = - 1 / k1
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

    fun contains(value: Float) = value in start..end
}

private data class AllPoints(val O: Point, val A: Point, val B: Point, val C: Point, val H: Point, val I: Point, val J: Point, val K:Point, val M: Point, val N: Point, val S: Point, val T: Point, val U: Point, val V: Point, val W: Point, val Z: Point) {
    fun toAbsSystem(absO: Point) = with(absO) {
        AllPoints(
            absScreenSystemWith(O),
            absScreenSystemWith(A),
            absScreenSystemWith(B),
            absScreenSystemWith(C),
            absScreenSystemWith(H),
            absScreenSystemWith(I),
            absScreenSystemWith(J),
            absScreenSystemWith(K),
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
        fun default(absO: Point) = AllPoints(absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy(), absO.copy())
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
            K.inCoordinateSystem(this),
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
            K.getSymmetricalPointAbout(this),
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
