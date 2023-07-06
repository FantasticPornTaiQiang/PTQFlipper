package ptq.mpga.pinance

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import ptq.mpga.pinance.ui.theme.PinanceTheme

private const val TAG = "PTQBookPageMainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PinanceTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val systemUiController = rememberSystemUiController()
                    val ctx = LocalContext.current

                    LaunchedEffect(Unit) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        systemUiController.setStatusBarColor(Color.Transparent, true)
                        systemUiController.setNavigationBarColor(Color.Transparent)
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        var clickCount by remember { mutableStateOf(0) }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PTQBookPageView Demo", fontSize = 24.sp, modifier = Modifier.clickable {
                                clickCount++
                                if (clickCount % 7 == 0) {
                                    Toast.makeText(ctx, "PTQ is Power", Toast.LENGTH_SHORT).show()
                                }
                            })
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

