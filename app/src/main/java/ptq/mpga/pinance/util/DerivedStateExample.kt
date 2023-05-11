package ptq.mpga.pinance.util

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

val TAG = "RememberExample"

@Composable
fun MyButton(state: MyButtonState, onClick: () -> Unit) {

}

data class MyButtonState(val color: Color, val radius: Float)

@Composable
fun rememberMyButtonState(myButtonState: MyButtonState) = remember(myButtonState) {
    mutableStateOf(myButtonState)
}

val colors = listOf(Color.Cyan, Color.Gray, Color.Red, Color.Blue, Color.Yellow, Color.Green)

//@Composable
//fun TodoList(highPriorityKeywords: List<String> = listOf("ptq", "power")) {
//
//    val todoTasks = remember { mutableStateListOf<String>() }
//
//    // 仅当highPriorityTasks或者todoTasks有变化时才计算highPriorityTasks，而并非每次重组都重新计算
//    val highPriorityTasks by remember(highPriorityKeywords) {
//        derivedStateOf {
//            todoTasks.filter {
//                highPriorityKeywords.any { keyword ->
//                    it.contains(keyword)
//                }
//            }
//        }
//    }
//
//    Box(Modifier.fillMaxSize()) {
//        LazyColumn(modifier = Modifier.fillMaxSize()) {
//            items(highPriorityTasks) {
//                Card(modifier = Modifier.fillMaxWidth(), contentColor = Color.Blue) {
//                    Text(text = it)
//                }
//            }
//            item {
//                Divider(modifier = Modifier.fillMaxWidth(), color = Color.LightGray)
//            }
//            items(todoTasks) {
//                Card(modifier = Modifier.fillMaxWidth(), contentColor = Color.Yellow) {
//                    Text(text = it)
//                }
//            }
//        }
//
//        Row(modifier = Modifier
//            .align(Alignment.BottomCenter)
//            .padding(vertical = 20.dp)) {
//            var input by remember {
//                mutableStateOf("")
//            }
//            TextField(value = input, onValueChange = {
//                input = it
//            })
//            Button(onClick = {
//                todoTasks.add(input)
//            }) {
//                Text("添加")
//            }
//        }
//    }
//}



//@Composable
//fun DerivedStateExample() {
//    val state = rememberLazyListState()
//
//    val currentGroup by remember {
//        derivedStateOf {
//            state.firstVisibleItemIndex / 50
//        }
//    }
//
//    LazyColumn(
//        state = state,
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        items(200) {
//            Text(
//                text = it.toString(),
//                Modifier
//                    .fillMaxWidth()
//                    .padding(10.dp)
//                    .background(Color.Yellow),
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//
//    ManyContents(current = currentGroup)
//}
//
//@Composable
//fun ManyContents(current: Int) {
//    Log.d(TAG, "DerivedStateExampleInner: aaa")
//    Text(text = current.toString())
//}

//@Composable
//fun DerivedStateExample() {
//    val input = remember {
//        mutableStateOf("")
//    }
//
//    val enabled = input.value.length >= 6
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center
//    ) {
//        DerivedStateExampleInner(input = input, enabled = enabled, onUserInput = {
//            input.value = it
//        })
//    }
//}
//
//@Composable
//fun DerivedStateExampleInner(input: State<String>, enabled: Boolean, onUserInput: (String) -> Unit) {
//    TextField(
//        value = input.value,
//        onValueChange = onUserInput)
//
//    Spacer(modifier = Modifier.height(height = 10.dp))
//
//    Log.d(TAG, "DerivedStateExample: 2")
//
//    Button(
//        onClick = { /*TODO*/ },
//        enabled = enabled
//    ) {
//        Text("登录")
//    }
//}
//@Composable
//fun DerivedStateExample() {
////    val scrollState = rememberLazyListState()
//////    val counter by remember {
//////        derivedStateOf {
//////            scrollState.firstVisibleItemIndex / 30
//////        }
//////    }
////    val counter = scrollState.firstVisibleItemIndex / 30
////
////    LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
////        items(300) {
////            Text(text = it.toString(),
////                Modifier
////                    .background(Color.Yellow)
////                    .padding(10.dp))
////        }
////    }
////
////
////    Text(text = counter.toString())
//    Example()
//}



//@Composable
//fun DerivedStateExample() {
//    val input = remember {
//        mutableStateOf("")
//    }
//
//    val enabled = remember {
//        derivedStateOf {
//            input.value.length >= 6
//        }
//    }
//
//    Log.d(TAG, "DerivedStateExample: 1")
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center
//    ) {
//        DerivedStateExampleInner(input = input, onUserInput = {
//            input.value = it
//        })
//
//        Spacer(modifier = Modifier.height(height = 10.dp))
//
//        Log.d(TAG, "DerivedStateExample: 2")
//
//        Button(
//            onClick = { /*TODO*/ },
//            enabled = enabled.value
//        ) {
//            Text("登录")
//        }
//    }
//}
//
//@Composable
//fun DerivedStateExampleInner(input: State<String>, onUserInput: (String) -> Unit) {
//    Log.d(TAG, "DerivedStateExample: 3")
//    TextField(
//        value = input.value,
//        onValueChange = onUserInput)
//}

//@Composable
//fun DerivedStateExample() {
//    val input = remember {
//        mutableStateOf("")
//    } //这里的input变成了State类型，而不再是通过by得到的String类型
//
//    val enabled = remember {
//        derivedStateOf {
//            input.value.length >= 6
//        }
//    } //enabled同样也变成了State类型
//
//    Log.d(TAG, "DerivedStateExample: 1")
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center
//    ) {
//        DerivedStateExampleInner(input = input, enabled = enabled, onUserInput = {
//            input.value = it
//        })
//    }
//}
//
//@Composable
//fun DerivedStateExampleInner(input: State<String>, enabled: State<Boolean>, onUserInput: (String) -> Unit) {
//    TextField(
//        value = input.value,
//        onValueChange = onUserInput)
//
//    Spacer(modifier = Modifier.height(height = 10.dp))
//
//    Log.d(TAG, "DerivedStateExample: 2")
//
//    Button(
//        onClick = { /*TODO*/ },
//        enabled = enabled.value
//    ) {
//        Text("登录")
//    }
//}