package ptq.mpga.pinance

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.transform
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.pinance.widget.PTQPageFlipper
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

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
                    PTQPageFlipper()
                }
            }
        }
    }
}

@Composable
fun WaveLoading(modifier: Modifier = Modifier, bitmap: ImageBitmap) {
    val waveDuration = 2000
    val transition = rememberInfiniteTransition()
    val animates = listOf(
        1f, 0.75f, 0.5f
    ).map {
        transition.animateFloat(
            initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
                animation = tween((it * waveDuration).roundToInt()),
                repeatMode = RepeatMode.Restart
            )
        )
    }
    var waveConfig by remember { mutableStateOf(WaveConfig(0.1f, 0f, 0.2f)) }
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val draggableState = rememberDraggableState {
        waveConfig = waveConfig.copy(progress = waveConfig.progress - it / screenHeight)
    }

    Canvas(modifier = modifier
        .fillMaxSize()
        .draggable(orientation = Orientation.Vertical, state = draggableState)) {
        drawWave(
            bitmap,
            waveConfig,
            animates
        )
    }
}

private fun DrawScope.drawWave(
    imageBitmap: ImageBitmap,
    waveConfig: WaveConfig,
    animates: List<State<Float>>,
) {
//    drawImage(image = imageBitmap, colorFilter = run {
//        val cm = ColorMatrix().apply { setToSaturation(0f) }
//        ColorFilter.colorMatrix(cm)
//    })

    animates.forEachIndexed { index, anim ->
        val maxWidth = 2 * size.width / waveConfig.velocity.coerceAtLeast(0.1f)
        val offsetX = maxWidth / 2 * (1 - anim.value)
//        Log.d(TAG, "drawWave: $maxWidth $offsetX ${anim.value}")
        translate(-offsetX) {
            drawPath(
                path = buildWavePath(
                    width = maxWidth,
                    height = size.height,
                    waveConfig = waveConfig,
                ), brush = ShaderBrush(ImageShader(imageBitmap).apply {
                    transform { postTranslate(offsetX, 0f) }
                }), alpha = if (index == 0) 1f else 0.5f
            )
        }

    }

}

val TAG = "aaa"

private fun buildWavePath(width: Float, height: Float, waveConfig: WaveConfig): Path {
    val amp = height * waveConfig.amp
    val progress = waveConfig.progress
    val drawWidth = width * 2
    val drawHeight = minOf(height * maxOf(0f, 1 - progress), amp) //实际调整后振幅
    val dp = 2f
    return Path().apply {
        reset()
        moveTo(0f, height)
        lineTo(0f, height * (1 - progress))
        if (progress > 0f && progress < 1f) {
            if (drawHeight > 0) {
                var x = dp
                while (x < drawWidth) {
                    lineTo(x, height * (1 - progress) - drawHeight / 2 * sin(8.0 * PI * x / drawWidth).toFloat())
                    x += dp
                }
            }
        }
        lineTo(drawWidth, height * (1 - progress))
        lineTo(drawWidth, height)
        close()
    }
}

data class WaveConfig(@FloatRange(from = 0.0, to = 1.0) val amp: Float, @FloatRange(from = 0.0, to = 1.0) val progress: Float, @FloatRange(from = 0.0, to = 1.0) val velocity: Float)
























