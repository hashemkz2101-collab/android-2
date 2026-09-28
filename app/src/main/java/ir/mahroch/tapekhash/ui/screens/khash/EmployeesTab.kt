package ir.mahroch.tapekhash.ui.screens.khash

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
import ir.mahroch.tapekhash.data.Session
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun EmployeesTab() {
    var employees by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf<JSONObject?>(null) }
    val isAdmin = Session.loadCachedUser()?.isAdmin == true
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            val res = ApiClient.call("getAllEmployeeBalances")
            val arr = res.getJSONArray("balances")
            employees = (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: ApiException) { error = e.message }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("نیروها و مانده حساب", style = MaterialTheme.typography.titleMedium)
            if (isAdmin) Button(onClick = { showAdd = true }) { Text("+ نیرو جدید") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(employees) { e ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(e.optString("name"), style = MaterialTheme.typography.titleSmall)
                            Text("سهم کل: ${fmt(e.optDouble("totalShare"))} — پرداختی: ${fmt(e.optDouble("totalPaid"))}")
                            Text("مانده: ${fmt(e.optDouble("balance"))}")
                        }
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    statement = ApiClient.call("getEmployeeStatement", JSONObject().put("name", e.optString("name")))
                                } catch (ex: ApiException) { error = ex.message }
                            }
                        }) { Text("ریز حساب") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("نیروی جدید") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("نام نیرو") }) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            ApiClient.call("addEmployee", JSONObject().put("name", newName.trim()))
                            newName = ""; showAdd = false
                            load()
                        } catch (e: ApiException) { error = e.message }
                    }
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("انصراف") } }
        )
    }

    statement?.let { st ->
        AlertDialog(
            onDismissRequest = { statement = null },
            title = { Text("ریز حساب ${st.optString("name")}") },
            text = {
                Column {
                    Text("مجموع سهم: ${fmt(st.optDouble("totalShare"))}")
                    Text("مجموع پرداختی: ${fmt(st.optDouble("totalPaid"))}")
                    Text("مانده: ${fmt(st.optDouble("balance"))}")
                }
            },
            confirmButton = { TextButton(onClick = { statement = null }) { Text("بستن") } }
        )
    }
}

fun fmt(n: Double): String {
    return "%,.0f".format(n)
}
