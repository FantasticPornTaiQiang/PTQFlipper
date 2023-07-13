package ptq.mpga.pinance

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.ptqbookpageview.widget.PTQBookPageView
import ptq.mpga.ptqbookpageview.widget.rememberPTQBookPageViewConfig
import ptq.mpga.ptqbookpageview.widget.rememberPTQBookPageViewState

class SharingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sharing)

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

                    var state by rememberPTQBookPageViewState(pageCount = 10, currentPage = 0)
                    val config by rememberPTQBookPageViewConfig(pageColor = Color(0xff123456))
                    val ctx = LocalContext.current

                    PTQBookPageView(state = state, config = config) {
                        onTurnPageRequest { currentPage, isNextOrPrevious, success ->
                            if (!success) {
                                Toast.makeText(ctx, if (isNextOrPrevious) "已经是最后一页啦" else "已经是第一页啦", Toast.LENGTH_SHORT).show()
                            } else {
                                state = state.copy(currentPage = if (isNextOrPrevious) currentPage + 1 else currentPage - 1)
                            }
                        }

                        contents { currentPage, refresh ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(config.pageColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (currentPage % 2 == 0) text1 else text2)
                            }

                            refresh()
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