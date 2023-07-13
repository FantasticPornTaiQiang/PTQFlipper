package ptq.mpga.ptqbookpageview.widget

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.annotation.IntRange

data class PTQBookPageViewState(@IntRange(from = 1L) val pageCount: Int, @IntRange(from = 0L) val currentPage: Int? = null) {
    init {
        require(pageCount > 0) {
            "pageCount必须大于0"
        }
        currentPage?.let {
            require(it < pageCount) {
                "currentPage范围必须在[0, currentPage)之间"
            }
        }

    }
}

/**
 * @param pageCount 总页数，如果页面总数小于当前页数，则会引发异常
 * @param currentPage 当前页数，如果为null则页数由翻页器内部控制
 */
@Composable
fun rememberPTQBookPageViewState(pageCount: Int = 1, currentPage: Int? = null) = remember {
    mutableStateOf(PTQBookPageViewState(pageCount, currentPage))
}

/**
 * @param pageColor 頁面顔色
 * @param disabled 是否禁用
 * @param distortionInterval 图像扭曲的采样间隔，可以根据屏幕大小去作适配，此值越小，曲线越精密，但同样地，计算开销会越大，性能会下降
 * @param bezierEdgeDownSampling 边缘扭曲的采样间隔，可以根据屏幕大小去作适配，此值越大，曲线越精密，但同样地，计算开销会越大，性能会下降
 */
@Composable
fun rememberPTQBookPageViewConfig(
    pageColor: Color = Color.White,
    disabled: Boolean = false,
    distortionInterval: Int = 25,
    bezierEdgeDownSampling: Int = 50,
) = remember {
    mutableStateOf(PTQBookPageViewConfig(pageColor, disabled, distortionInterval, bezierEdgeDownSampling))
}

data class PTQBookPageViewConfig(
    val pageColor: Color = Color.White,
    val disabled: Boolean = false,
    val distortionInterval: Int = 25,
    val bezierEdgeDownSampling: Int = 50,
)

internal val LocalPTQBookPageViewConfig = compositionLocalOf<PTQBookPageViewConfig> { error("Local flipper config error") }