package ptq.mpga.pinance

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.pinance.widget.PTQBookPageView
import ptq.mpga.pinance.widget.rememberPTQBookPageViewConfig
import ptq.mpga.pinance.widget.rememberPTQBookPageViewState
import kotlin.random.Random

private const val TAG = "PTQBookNovelActivity"

class NovelActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PinanceTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val systemUiController = rememberSystemUiController()

                    LaunchedEffect(Unit) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        systemUiController.setStatusBarColor(Color.Transparent, true)
                        systemUiController.setNavigationBarColor(Color.Transparent)
                    }

                    PTQView()
                }
            }
        }
    }
}

private val pageColorList = listOf(Color(0xffbfefff), Color(0xfff5f5f5), Color(0xffffa07a), Color(0xffff6eb4), Color(0xffc1ffc1))

private val stickerList = arrayOf(
    R.drawable.sticker49,
    R.drawable.sticker50,
    R.drawable.sticker51,
    R.drawable.sticker52,
    R.drawable.sticker53,
    R.drawable.sticker59,
    R.drawable.sticker128,
    R.drawable.sticker129,
    R.drawable.sticker130,
    R.drawable.sticker131,
    R.drawable.sticker132,
    R.drawable.sticker133,
    R.drawable.sticker145,
    R.drawable.sticker146,
    R.drawable.sticker147,
    R.drawable.sticker148,
)

private data class Sticker(val id: Int, val resourceId: Int, val page: Int, val offset: Offset, val size: Size)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PTQView() {
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val screenW = LocalConfiguration.current.screenWidthDp.dp

    val ctx = LocalContext.current
    var state by rememberPTQBookPageViewState(pageCount = 100, currentPage = 0)
    var config by rememberPTQBookPageViewConfig(pageColor = Color(0xfff5f5f5))

    var padding by remember { mutableStateOf(arrayOf(0f, 0f, 0f, 0f)) }
    var diySize by remember { mutableStateOf(false) }

    val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    val stickers = remember { mutableStateListOf<Sticker>() }

    PTQBookPageView(
        modifier = Modifier.padding(start = padding[0].dp, top = padding[1].dp, end = padding[2].dp, bottom = padding[3].dp),
        config = config,
        state = state,
        ptqBookPageViewScope = {
            onTurnPageRequest { currentPage, isNextOrPrevious, success ->
                if (!success) {
                    Toast.makeText(ctx, if (isNextOrPrevious) "已经是最后一页啦" else "已经是第一页啦", Toast.LENGTH_SHORT).show()
                } else {
                    state = state.copy(currentPage = if (isNextOrPrevious) currentPage + 1 else currentPage - 1)
                }
            }

            tapBehavior { leftUp, rightDown, touchPoint ->
                val middle = (rightDown - leftUp).x / 2
                return@tapBehavior touchPoint.x > middle
            }

            responseDragWhen { rightDown, startTouchPoint, currentTouchPoint ->
                return@responseDragWhen currentTouchPoint.x < startTouchPoint.x
            }

            dragBehavior { rightDown, initialTouchPoint, lastTouchPoint, isRightToLeftWhenStart ->
                val isFingerAtRight = lastTouchPoint.x > rightDown.x / 2

                var isNext: Boolean? = null
                if (isRightToLeftWhenStart && !isFingerAtRight) {
                    isNext = true
                }

                if (!isRightToLeftWhenStart && isFingerAtRight) {
                    isNext = false
                }

                return@dragBehavior Pair(!isFingerAtRight, isNext)
            }

            contents { currentPage, refresh ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(config.pageColor)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 18.dp)) {
                        when(currentPage) {
                            0 -> {
                                Column {
                                    Text(text1, lineHeight = 24.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp))
                                }
                            }
                            1 -> {
                                Column {
                                    Text(text2, lineHeight = 24.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp))
                                }
                            }
                            else -> {
                                Text(
                                    text = (currentPage + 1).toString(),
                                    fontSize = 30.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    StickerView(stickers.filter {
                        it.page == currentPage
                    }, onOffsetChanged = { which, offset ->
                        stickers[which] = stickers[which].copy(offset = offset)
                    }, onStickerCreate = { resource ->
                        stickers += Sticker(stickers.size, resource, currentPage, Offset(screenWidthPx / 2, screenHeightPx / 2), Size(screenWidthPx / 5, screenWidthPx / 5))
                    }, onSizeChanged = { which, size ->
                        stickers[which] = stickers[which].copy(size = size)
                    })

                    LazyRow(
                        modifier = Modifier
                            .padding(bottom = 40.dp)
                            .background(color = Color.Gray.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .align(Alignment.BottomCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        item {
                            Button(
                                onClick = { (ctx as NovelActivity).onBackPressed() }, modifier = Modifier
                                    .padding(end = 10.dp), shape = RoundedCornerShape(5.dp)
                            ) {
                                Text("返回")
                            }
                            Text(text = (currentPage + 1).toString() + " / " + state.pageCount, color = Color.White, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    padding = if (!diySize) {
                                        val Ox = Random.nextInt(0, (screenW * 0.2f).value.toInt()).toFloat()
                                        val Oy = Random.nextInt(0, (screenH * 0.2f).value.toInt()).toFloat()
                                        val Cx = Random.nextInt(0, (screenW * 0.2f).value.toInt()).toFloat()
                                        val Cy = Random.nextInt(0, (screenH * 0.2f).value.toInt()).toFloat()
                                        arrayOf(Ox, Oy, Cx, Cy)
                                    } else {
                                        arrayOf(0f, 0f, 0f, 0f)
                                    }
                                    diySize = !diySize
                                }, modifier = Modifier.padding(start = 15.dp), shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(if (!diySize) "换个尺寸" else "还原")
                            }
                            Button(
                                onClick = { config = config.copy(pageColor = pageColorList.random()) }, modifier = Modifier
                                    .padding(start = 15.dp), shape = RoundedCornerShape(5.dp)
                            ) {
                                Text("换个底色")
                            }
                            Button(
                                onClick = { state = state.copy(currentPage = (0 until state.pageCount).toMutableList().random()) }, modifier = Modifier
                                    .padding(start = 10.dp), shape = RoundedCornerShape(5.dp)
                            ) {
                                Text("随机翻页")
                            }
                            Button(
                                onClick = { config = config.copy(disabled = !config.disabled) }, modifier = Modifier
                                    .padding(start = 15.dp), shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(if (!config.disabled) "禁用翻页" else "启用翻页")
                            }
                            Button(
                                onClick = { state = state.copy(pageCount = currentPage + (1..100).toMutableList().random()) }, modifier = Modifier
                                    .padding(start = 10.dp), shape = RoundedCornerShape(5.dp)
                            ) {
                                Text("改变总页数")
                            }
                        }
                    }
                }

                refresh()
            }

        })
}

private const val stickerColumnCount = 4

@Composable
private fun StickerView(currentPageStickers: List<Sticker>, onOffsetChanged: (Int, Offset) -> Unit, onSizeChanged: (Int, Size) -> Unit, onStickerCreate: (Int) -> Unit) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    val ctx = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Transparent)
    ) {
        Button(onClick = {
            if (currentPageStickers.size == 3) {
                Toast.makeText(ctx, "这一页已经不能再贴啦", Toast.LENGTH_SHORT).show()
                return@Button
            }
            showDialog = true
        }, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text("贴纸")
        }

        if (currentPageStickers.isNotEmpty()) {
            for (i in currentPageStickers.indices) {
                val item = currentPageStickers[i]
                var offset by remember(item) { mutableStateOf(item.offset) }
                var size by remember(item) { mutableStateOf(item.size) }

                with(LocalDensity.current) {

                    Box(modifier = Modifier
                        .offset(offset.x.toDp(), offset.y.toDp())
                        .width(size.width.toDp())
                        .height(size.height.toDp())) {
                        Image(painter = painterResource(id = item.resourceId), contentDescription = null, modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(item) {
                                detectDragGestures(onDrag = { _, delta ->
                                    offset += delta
                                }, onDragEnd = {
                                    onOffsetChanged(item.id, offset)
                                })
                            })
                        Box(modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .height(20.dp)
                            .width(20.dp)
                            .pointerInput(item) {
                                detectDragGestures(onDrag = { _, delta ->
                                    with(size) {
                                        size = Size(width + delta.x, height + delta.y)
                                    }
                                }, onDragEnd = {
                                    onSizeChanged(item.id, size)
                                })
                            })
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = {}) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(stickerList.size / stickerColumnCount) { index ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (j in 0 until stickerColumnCount) {
                            val which = stickerList[index * stickerColumnCount + j]
                            Image(painter = painterResource(id = which), contentDescription = null, modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onStickerCreate(which)
                                    showDialog = false
                                })
                        }
                    }
                }
            }
        }
    }
}

private const val text1 = "晨曦的微光透过半开都窗户衍射进屋内\n" +
        "\n" +
        "温柔的映照在心海粉嫩的肚皮上\n" +
        "\n" +
        "枕头早已被她的口水润湿……\n" +
        "\n" +
        "“旅行者……嘿嘿……我的旅行者……让我好好……尝一下你的味道吧……嘿嘿……旅行者……嘿嘿……”心海面带笑容说着梦话\n" +
        "\n" +
        "“珊瑚宫大人，大家已经准备好今天的早会了，您看……”\n" +
        "\n" +
        "“不去不去，周末就要好好休息，给他们放假就说这是命令”\n" +
        "\n" +
        "“可是今天的来客有远道而来的旅行者”\n" +
        "\n" +
        "“旅行者!!!”\n" +
        "\n" +
        "“我马上去开会，让大家等我五分钟”\n" +
        "\n" +
        "第一次见到旅行者的时候，她就深深的爱上了他，只可惜一直没有合适的机会，现在不就是“煮熟的鸭子送到嘴边了嘛”\n" +
        "\n" +
        "为此，她特意换了一身“**”的衣服\n" +
        "\n" +
        "开完会后，心海以工作的借口拉空出去散步\n" +
        "\n" +
        "“什么事啊 ，心海”\n" +
        "\n" +
        "“你别问，跟我走就是了，是工作的任务哦”\n" +
        "\n" +
        "心海拉着空来到一家酒馆，她显然对这里的老板很熟\n" +
        "\n" +
        "到老板耳边悄悄的说：你懂的，酒里面，待会别忘了加点“*”哦\n" +
        "\n" +
        "老板点头示意后，心海拉着空来到桌边\n" +
        "\n" +
        "“来陪我喝点，平时工作很累的，喝点酒放松一下”\n" +
        "\n" +
        "“好……好吧，空无奈的拿起酒杯”\n" +
        "\n" +
        "不出所料，空刚喝一口就被“醉倒”了\n" +
        "\n" +
        "心海抱着空来到一家客栈\n" +
        "\n" +
        "“旅行者，你是不知道我有多喜欢你啊，终于落到我的手里了……嘿嘿……，让我好好尝尝你的……吧旅行者……嘿嘿……”"

private val text2 = "“嘛~也不是不能说，就是亲爱的要先答应我，不论心海说了什么今晚都要好好睡觉哦！”，心海脸红不敢看空小声说道。\n" +
        "\n" +
        "“嗯？行吧？”，空犹豫了一下说道，总感觉会对自己的尊严产生一些些影响！但说起来心海某些地方的资本真的是很夸张啊~在捏一把！\n" +
        "\n" +
        "“那个亲爱最近是不是肾虚啊~感觉不管是时间还是次数都跟不上曾经了~要去看看医生吗？”，心海十分认真地说道，只不过配上那眼含春水弱气的样子很难不唤起空的欲望。\n" +
        "\n" +
        "“……”，空沉默了……很明显他作为男人的尊严被心海三言两语给挑战了！虚还不是因为你之前留下来的心理阴影太大了！\n" +
        "\n" +
        "“亲爱的…”，心海扬头看向空无辜的大眼睛扑扇着。\n" +
        "\n" +
        "“呼~心海啊~”，空愣了一会气场变得犹豫起来，叹了口气望着怀中的伊人，眼神变得坚定起来，那是一种破后而立的成长！是啊！自己总是沉浸在过去的阴影中走不出去，才让两人的关系变得如现在这般畸形，所以该有所改变了！\n" +
        "\n" +
        "至于第一步那当然是确认家庭地位了，就比如让某个嘲笑自己肾虚的家伙下不了床！\n" +
        "\n" +
        "“怎么了？”，心海问道，默默将病娇心海的意识给放了出来，然后静静等待惩罚到来。"