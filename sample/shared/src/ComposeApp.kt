import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import kotlinx.coroutines.launch

@Composable
fun ComposeApp() {
    var url by remember { mutableStateOf("https://www.google.com/") }
    var htmlContent by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                UrlInputField(url) { url = it }
                Spacer(modifier = Modifier.size(8.dp))
                LoadButton(url) { htmlContent = it }
                Spacer(modifier = Modifier.size(8.dp))
                HtmlContentDisplay(htmlContent)
            }
        }
    }
}

@Composable
fun UrlInputField(url: String, onUrlChange: (String) -> Unit) {
    Row {
        TextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.weight(1f),
            label = { Text("URL") }
        )
    }
}

@Composable
fun LoadButton(url: String, onLoadContent: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        onLoadContent("Loading...")
        scope.launch {
            try {
                val doc = Ksoup.parseGetRequest(url)
                val title = doc.title()
                val content = "Page Title: $title\n\nPage Body: ${doc.body()}"
                onLoadContent(content)
            } catch (e: Exception) {
                onLoadContent("Failed to load content: ${e.message}")
            }
        }
    }) {
        Text("Load")
    }
}

@Composable
fun HtmlContentDisplay(htmlContent: String) {
    LazyColumn {
        item {
            Text(htmlContent, modifier = Modifier.fillMaxSize())
        }
    }
}