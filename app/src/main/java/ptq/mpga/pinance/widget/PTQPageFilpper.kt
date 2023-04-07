package ptq.mpga.pinance.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "PTQPageFlipper"

data class FlipperConfig(
    val pageColor: Color,
)

val LocalFlipperConfig = compositionLocalOf<FlipperConfig> { error("Local flipper config error") }

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PTQPageFlipper(modifier: Modifier = Modifier, pageColor: Color = Color.White, onNext: () -> Unit = {}, onPrevious: () -> Unit = {}, content: @Composable BoxScope.() -> Unit) {

    Box(
        modifier.fillMaxSize()
    ) {

        var _size by remember { mutableStateOf(IntSize.Zero) }

        var _bitmap by remember {
            mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565))
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                object : AbstractComposeView(context) {

                    @Composable
                    override fun Content() {
                        Box(
                            Modifier
                                .wrapContentSize()
                                .onSizeChanged {
                                    _size = it
                                }) {

                            content()
                        }
                    }

                    override fun dispatchDraw(canvas: Canvas?) {
                        if (width == 0 || height == 0) return
                        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas2 = Canvas(source)
                        super.dispatchDraw(canvas2)
                        _bitmap = source
                    }

                }
            }

        )

        CompositionLocalProvider(
            LocalFlipperConfig provides FlipperConfig(pageColor = pageColor)
        ) {
            Box(
                Modifier
                    .width(_size.width.dp)
                    .height(_size.height.dp)
                    .align(Alignment.Center)
                    .clipToBounds()
            ) {
                PTQPageFlipperInner(bitmap = _bitmap, onNext, onPrevious)
            }
        }

    }



}