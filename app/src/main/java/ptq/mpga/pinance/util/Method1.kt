import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.recyclerview.widget.RecyclerView

data class RequestConfig(val api: String, val bodyJson: String, val method: String = "POST")
data class Data(val myData1: Int, val myData2: Boolean)

val default = RequestConfig("/user/info", "")

//模拟网络请求，获取数据
fun fetchData(requestConfig: RequestConfig, onSuccess: (data: Data) -> Unit = {}, onFailure: (errorMsg: String) -> Unit = {}) {
    //假设调用更底层如Retrofit等模块，成功拿到数据后
    onSuccess(Data(1, true))

    //或者
    //失败后
    onFailure("断网啦")
}

//UI层
@Composable
fun MyView() {
    Button(onClick = {
        //点击按钮后发送网络请求
        fetchData(requestConfig = default, onSuccess = {
            //更新UI
        }, onFailure = {
            //弹Toast提示用户
        })
    }) {

    }
}

fun retrofitRequest(requestConfig: RequestConfig) = MyResult(200, "", Data(1, false))

data class MyResult(val code: Int, val msg: String, val data: Data) {
    fun onSuccess(block: (data: Data) -> Unit) = this.also {
        if (code == 200) { //判断交给MyResult，若code==200，则认为成功
            block(data)
        }
    }

    fun onFailure(block: (errorMsg: String) -> Unit) = this.also {
        if (code != 200) { //判断交给MyResult，若code!=200，则认为失败
            block(msg)
        }
    }
}

//模拟网络请求，获取数据
fun fetchData(requestConfig: RequestConfig): MyResult {
    return retrofitRequest(requestConfig)
}

//UI层
@Composable
fun MyView1() {
    Button(onClick = {
        //点击按钮后发送网络请求
        fetchData(requestConfig = RequestConfig("/user/info", "")).onSuccess {
            //更新UI
        }.onFailure {
            //弹Toast提示用户
        }
    }) {

    }
}

interface ResultScope {
    fun onSuccess(block: (data: Data) -> Unit)
    fun onFailure(block: (errorMsg: String) -> Unit)
}

internal class ResultScopeImpl : ResultScope {
    var onSuccessBlock: (data: Data) -> Unit = {}
    var onFailureBlock: (errorMsg: String) -> Unit = {}

    override fun onSuccess(block: (data: Data) -> Unit) {
        onSuccessBlock = block
    }

    override fun onFailure(block: (errorMsg: String) -> Unit) {
        onFailureBlock = block
    }
}

//模拟网络请求，获取数据
fun fetchData2(requestConfig: RequestConfig, resultScope: ResultScope.() -> Unit = {}) {
    val result = retrofitRequest(requestConfig)
    val resultScopeImpl = ResultScopeImpl().apply(resultScope)

    resultScopeImpl.run {
        if (result.code == 200) onSuccessBlock(result.data) else onFailureBlock(result.msg)
    }


}


//UI层
@Composable
fun MyView2() {
    Button(onClick = {
        //点击按钮后发送网络请求
        fetchData2(requestConfig = RequestConfig("/user/info", "")) {
            onSuccess {
                //更新UI
            }
            onFailure {
                //弹Toast提示用户
            }
        }
    }) {

    }
}