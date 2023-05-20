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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import ptq.mpga.pinance.ui.theme.PinanceTheme
import ptq.mpga.pinance.widget.PTQBookPageView
import ptq.mpga.pinance.widget.rememberPTQBookPageViewConfig
import ptq.mpga.pinance.widget.rememberPTQBookPageViewState

private val albumList = arrayOf(
    R.drawable.album1,
    R.drawable.album2,
    R.drawable.album3,
    R.drawable.album4,
    R.drawable.album5,
    R.drawable.album6,
    R.drawable.album7,
)

class AlbumActivity : AppCompatActivity() {
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
                        systemUiController.setStatusBarColor(Color.Transparent)
                        systemUiController.setNavigationBarColor(Color.Transparent)
                    }

                    var state by rememberPTQBookPageViewState(pageCount = albumList.size, currentPage = 0)
                    var config by rememberPTQBookPageViewConfig(pageColor = Color(0xfff5f5f5))
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
                                Image(painter = painterResource(id = albumList[currentPage]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

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
                                            onClick = { onBackPressed() }, modifier = Modifier
                                                .padding(end = 10.dp), shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text("返回")
                                        }
                                        Text(text = (currentPage + 1).toString() + " / " + state.pageCount)
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
                                    }
                                }
                            }

                            refresh()
                        }
                    }
                }
            }
        }
    }
}