package ptq.mpga.pinance.widget

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.reflect.KProperty

/**
 * @param pageCount 总页数
 * @param currentPage 当前页数，如果为null则页数由翻页器内部控制
 * @param disabled 是否禁用
 */
data class PTQBookPageViewState(@IntRange(from = 1L) val pageCount: Int, @IntRange(from = 0L) val currentPage: Int? = null, val disabled: Boolean = false) {
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
 * @param disabled 是否禁用
 */
@Composable
fun rememberPTQBookPageViewState(pageCount: Int = 1, currentPage: Int? = null, disabled: Boolean = false) = remember {
    mutableStateOf(PTQBookPageViewState(pageCount, currentPage, disabled))
}