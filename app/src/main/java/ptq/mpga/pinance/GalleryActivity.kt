package ptq.mpga.pinance

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import ptq.mpga.ptqbookpageview.widget.PTQBookPageView
import ptq.mpga.ptqbookpageview.widget.rememberPTQBookPageViewConfig
import ptq.mpga.ptqbookpageview.widget.rememberPTQBookPageViewState

private val galleryList = arrayOf(
    R.drawable.gallery1,
    R.drawable.gallery2,
    R.drawable.gallery3,
    R.drawable.gallery4,
    R.drawable.gallery5,
    R.drawable.gallery6,
    R.drawable.gallery7,
    R.drawable.gallery8,
    R.drawable.gallery9,
    R.drawable.gallery10,
    R.drawable.gallery11,
    R.drawable.gallery12,
    R.drawable.gallery13,
    R.drawable.gallery14,
    R.drawable.gallery15,
    R.drawable.gallery16,
)

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PinanceTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val systemUiController = rememberSystemUiController()

                    LaunchedEffect(Unit) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        systemUiController.setStatusBarColor(Color.Transparent, true)
                        systemUiController.setNavigationBarColor(Color.Transparent)
                    }

                    var state by rememberPTQBookPageViewState(pageCount = galleryList.size, currentPage = 0)
                    var config by rememberPTQBookPageViewConfig(pageColor = Color(0xfffafafa))
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
                                Image(painter = painterResource(id = galleryList[currentPage]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

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
                                            onClick = { finish() }, modifier = Modifier
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