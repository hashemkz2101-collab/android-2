package ir.mahroch.tapekhash.ui.screens.tape

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mahroch.tapekhash.data.ApiClient
import ir.mahroch.tapekhash.data.ApiException
import ir.mahroch.tapekhash.data.TapeRow
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val STATUS_IN_USE = "در حال استفاده"

/** لیست همه‌ی تپه‌ها با فیلتر متنی و فیلتر وضعیت. فیلتر سمت اپ انجام می‌شود. */
@Composable
fun TapeListTab() {
    var all by remember { mutableStateOf<List<TapeRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("") }
    var onlyFree by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true; error = null
        try {
            val res = ApiClient.call("listTapes", JSONObject())
            val arr = res.getJSONArray("results")
            all = (0 until arr.length()).map { TapeRow.fromJson(arr.getJSONObject(it)) }
        } catch (e: ApiException) {
            error = e.message
        } catch (e: Exception) {
            error = "خطا در ارتباط با سرور."
        }
        loading = false
    }

    fun act(action: String, row: TapeRow) {
        scope.launch {
            try {
                ApiClient.call(action, JSONObject().put("id", row.id))
                load()
            } catch (e: ApiException) { error = e.message }
        }
    }

    LaunchedEffect(Unit) { load() }

    val shown = remember(all, filter, onlyFree) {
        val q = filter.trim()
        all.filter { r ->
            (q.isEmpty() || r.tape.contains(q, ignoreCase = true) || r.box.contains(q, ignoreCase = true)) &&
                (!onlyFree || r.status != STATUS_IN_USE)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filter, onValueChange = { filter = it },
                label = { Text("فیلتر بر اساس کد تپه یا باکس") },
                modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { scope.launch { load() } }) { Text("تازه‌سازی") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = onlyFree, onCheckedChange = { onlyFree = it })
            Text("فقط تپه‌های آزاد")
        }
        Text("تعداد: ${shown.size} از ${all.size}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { row ->
                TapeCard(
                    row = row,
                    onTake = { act("takeTape", row) },
                    onReturn = { act("returnTape", row) }
                )
            }
        }
    }
}
