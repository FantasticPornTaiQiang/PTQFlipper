package ptq.mpga.pinance

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.pinance.widget.PTQBookPageView

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PinanceTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val all = remember {
                        mutableStateListOf(
                            R.drawable.xinhai1, R.drawable.xinhai2,
                            R.drawable.xinhai3, R.drawable.xinhai4, R.drawable.xinhai5, R.drawable.xinhai6, R.drawable.xinhai7, R.drawable.xinhai8, R.drawable.xinhai9, R.drawable.ptq
                        )
                    }
//                    Test(Color.Green)

                    PTQBookPageView(pageCount = all.size) {
                        contents { index ->
//                            when (index) {
//                                0 -> {
//                                    Column(modifier = Modifier.fillMaxSize()) {
//                                        Image(painter = painterResource(id = R.drawable.xinhai2), contentDescription = "xinhai2")
//                                        Text(text = text2)
//                                    }
//                                }
//                                1 -> {
//                                    Text(text = text, modifier = Modifier.fillMaxSize())
//                                }
//                                2 -> {
//                                    Image(painter = painterResource(id = R.drawable.xinhai1), contentDescription = "xinhai", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
//                                }
//                            }

                            Box(Modifier.fillMaxSize()) {
                                Image(painter = painterResource(id = all[index]), contentDescription = "xinhai", modifier = Modifier
                                    .wrapContentSize(Alignment.Center)
                                    .align(Alignment.Center))

                                Text(
                                    text = (index + 1).toString() + " / " + all.size,
                                    Modifier
                                        .padding(bottom = 40.dp)
                                        .background(color = Color.Gray.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .align(Alignment.BottomCenter)
                                )
                            }
                        }

                        onPageWantToChange { _, nextOrPrevious, success ->
                            if (!success) {
                                Toast.makeText(this@MainActivity, if (nextOrPrevious) "已经是最后一页啦" else "已经是第一页啦", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                }
            }
        }
    }
}

//@Composable
//fun Test() {
//    val colorList = arrayOf(Color.Black, Color.Gray, Color.Green, Color.Red, Color.Cyan)
//
//    var color by remember {
//        mutableStateOf(Color.Black)
//    }
//
//    Box(modifier = Modifier
//        .fillMaxSize()
//        .drawWithCache {
//            onDrawWithContent {
//                drawCircle(color, radius = 50f, center = Offset.Zero)
//                drawContent()
//            }
//        }) {
//        Button(onClick = { color = colorList.random(); Log.d("aaa", "Test: aaa") }, modifier = Modifier.align(Alignment.Center)) {
//            Text("点我")
//        }
//    }
//}

@Composable
fun Test(color: Color) {
    AndroidView(factory = {
        object : AbstractComposeView(it) {

            @Composable
            override fun Content() {
                var text by remember { mutableStateOf(0L) }

                Button(onClick = { text = System.currentTimeMillis() }) {
                    Text(text = text.toString(), color = color)
                }
            }

        }
    })
}


//@Composable
//fun Test() {
//    Log.d("aaa", "Test: 3")
//    Trigger()
//
//    Box {
//        Log.d("aaa", "Test: 2")
//    }
//}
//
//@Composable
//fun Trigger() {
//    var trigger by remember { mutableStateOf(false) }
//    Box {
//
//        Log.d("aaa", "Test: 1")
//        Button(onClick = {
//            trigger = !trigger
//        }) {
//
//        }
//    }
//}


private const val text = "晨曦的微光透过半开都窗户衍射进屋内\n" +
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



















