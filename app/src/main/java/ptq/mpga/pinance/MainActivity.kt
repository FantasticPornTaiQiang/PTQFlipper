package ptq.mpga.pinance

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.pinance.widget.PTQBookPageView
import ptq.mpga.pinance.widget.rememberPTQBookPageViewConfig
import ptq.mpga.pinance.widget.rememberPTQBookPageViewState
import kotlin.random.Random

private const val TAG = "PTQBookPageMainActivity"


class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PinanceTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val systemUiController = rememberSystemUiController()

                    LaunchedEffect(Unit) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        systemUiController.setStatusBarColor(Color.Transparent, true)
                        systemUiController.setNavigationBarColor(Color.Transparent)
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PTQBookPageView Demo", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(modifier = Modifier.fillMaxWidth(), onClick = { startActivity(Intent(this@MainActivity, NovelActivity::class.java)) }) {
                                Text("小说", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(modifier = Modifier.fillMaxWidth(), onClick = { startActivity(Intent(this@MainActivity, AlbumActivity::class.java)) }) {
                                Text("画册", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(modifier = Modifier.fillMaxWidth(), onClick = { startActivity(Intent(this@MainActivity, GalleryActivity::class.java)) }) {
                                Text("画廊", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }
}

