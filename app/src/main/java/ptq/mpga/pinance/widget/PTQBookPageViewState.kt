package ptq.mpga.pinance.widget

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlin.reflect.KProperty

/**
 * @param pageCount 总页数
 * @param currentPage 当前页数，如果为null则页数由翻页器内部控制
 */
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
 * @param pageCount 总页数
 * @param currentPage 当前页数，如果为null则页数由翻页器内部控制
 */
@Composable
fun rememberPTQBookPageViewState(pageCount: Int = 1, currentPage: Int? = null) = remember {
    mutableStateOf(PTQBookPageViewState(pageCount, currentPage))
}

/**
 * @param pageColor 頁面顔色
 * @param disabled 是否禁用
 */
@Composable
fun rememberPTQBookPageViewConfig(pageColor: Color = Color.White, disabled: Boolean = false) = remember {
    mutableStateOf(PTQBookPageViewConfig(pageColor, disabled))
}