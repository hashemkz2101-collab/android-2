package ir.mahroch.tapekhash.ui.screens.khash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mahroch.tapekhash.data.ApiClient
import ir.mahroch.tapekhash.data.ApiException
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsTab() {
    var employeeNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedEmployee by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var jalaliDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var payments by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun loadPayments() {
        try {
            val res = ApiClient.call("getPayments")
            val arr = res.getJSONArray("payments")
            payments = (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) { }
    }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.call("getActiveEmployeeNames")
            val arr = res.getJSONArray("names")
            employeeNames = (0 until arr.length()).map { arr.getString(it) }
            if (employeeNames.isNotEmpty()) selectedEmployee = employeeNames[0]
        } catch (e: Exception) { }
        loadPayments()
    }

    fun submit() {
        error = null; message = null
        if (selectedEmployee.isBlank() || amount.isBlank()) {
            error = "نیرو و مبلغ را وارد کنید."
            return
        }
        scope.launch {
            try {
                val body = JSONObject().put("employeeName", selectedEmployee).put("amount", amount).put("description", description)
                if (jalaliDate.isNotBlank()) body.put("jalaliDate", jalaliDate)
                val res = ApiClient.call("createPayment", body)
                message = "پرداخت ثبت شد. مانده جدید: ${fmt(res.optDouble("balance"))}"
                amount = ""; description = ""; jalaliDate = ""
                loadPayments()
            } catch (e: ApiException) { error = e.message }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("ثبت پرداخت به نیرو", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedEmployee, onValueChange = {}, readOnly = true,
                label = { Text("نیرو") }, modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                employeeNames.forEach { name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { selectedEmployee = name; expanded = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("مبلغ") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = jalaliDate, onValueChange = { jalaliDate = it }, label = { Text("تاریخ شمسی (خالی = امروز)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())

        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.primary) }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) { Text("ثبت پرداخت") }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(12.dp))
        Text("پرداخت‌های اخیر", style = MaterialTheme.typography.titleMedium)
        payments.forEach { p ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text("${p.optString("employee_name")} — ${fmt(p.optDouble("amount"))}")
                    Text("تاریخ: ${p.optString("jalali_date")}")
                }
            }
        }
    }
}
