package ptq.mpga.pinance

import android.app.Application
import android.util.Log
import top.shixinzhang.bitmapmonitor.BitmapMonitor

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
//        BitmapMonitor.init(
//            BitmapMonitor.Config.Builder()
//                .checkRecycleInterval(1)
//                .getStackThreshold(1)
//                .restoreImageThreshold(Long.MAX_VALUE)
//                .restoreImageDirectory(getExternalFilesDir("bitmap_monitor")?.absolutePath) //获取堆栈的阈值，当一张图片占据的内存超过这个数值后就会去抓栈 //保存还原后图片的目录
//                .showFloatWindow(true)                  //是否展示悬浮窗，可实时查看内存大小（建议只在 debug 环境打开）
//                .isDebug(true)
//                .context(this)
//                .build()
//        )
//
////        BitmapMonitor.start()
//        BitmapMonitor.addListener {
//            Log.d("bitmapMonitor", "onBitmapInfoChanged: $it")
//        }
    }
}