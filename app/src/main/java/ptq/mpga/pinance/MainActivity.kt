package ptq.mpga.pinance

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.transform
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.pinance.widget.PTQPageFlipper
import ptq.mpga.pinance.widget.PTQPageFlipperInner
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PinanceTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
//                    Box(modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color.Black)) {
//                        WaveLoading(modifier = Modifier.fillMaxSize(), ImageBitmap.imageResource(id = R.drawable.ptq))
//                    }
                    PTQPageFlipper {
                        Column(modifier = Modifier.padding(horizontal = 0.dp, vertical = 15.dp)) {
//                            Text(text = text)
                            Image(painter = painterResource(id = R.drawable.xinhai1), contentDescription = "xinhai", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        }
                    }
                }
            }
        }
    }
}

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





















