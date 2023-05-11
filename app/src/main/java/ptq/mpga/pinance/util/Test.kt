package ptq.mpga.pinance.util

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CountDisplay(count: State<Int>) {
    Log.d(TAG, "CountDisplay: 1")
    Text("Count: ${count.value}")
}

@Composable
fun DerivedStateExample() {
    val sum = remember {
        mutableStateOf(0)
    }

    Column {
        CountDisplay(sum)

        Log.d(TAG, "Example: $")

        Button(onClick = {
            sum.value++
        }) {

        }
    }
}

@Composable
fun CountDisplay1(count: State<Int>) {
    Text("Count: ${count.value}")
}

@Composable
fun DerivedStateExample1() {
    var a by remember { mutableStateOf(0) }
    var b by remember { mutableStateOf(0) }
    val sum = remember {
        derivedStateOf {
            a + b
        }
    }

    Log.d(TAG, "DerivedStateExample: ${sum.hashCode()}")
    // Changing either a or b will cause CountDisplay to recompose but not trigger Example
    // to recompose.
    CountDisplay1(sum)

    Log.d(TAG, "Example: $")

    Button(onClick = {
        a++
        b += 2
    }) {

    }
}