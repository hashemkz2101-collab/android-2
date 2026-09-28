package ir.mahroch.tapekhash.ui.screens.khash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mahroch.tapekhash.data.ApiClient
import org.json.JSONObject

@Composable
fun ReportsTab() {
    var summary by remember { mutableStateOf<JSONObject?>(null) }
    var dashboard by remember { mutableStateOf<JSONObject?>(null) }
    var financeDashboard by remember { mutableStateOf<JSONObject?>(null) }
    var byDate by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    LaunchedEffect(Unit) {
        try { summary = ApiClient.call("getKhashSummaryReport") } catch (e: Exception) { }
        try { dashboard = ApiClient.call("getKhashDashboard") } catch (e: Exception) { }
        try { financeDashboard = ApiClient.call("getFinanceDashboard") } catch (e: Exception) { }
        try {
            val res = ApiClient.call("getKhashReportByDate")
            val arr = res.getJSONArray("report")
            byDate = (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) { }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("داشبورد امروز", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        financeDashboard?.let { f ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("تعداد فاکتور امروز: ${f.optInt("todayInvoiceCount")}")
                    Text("مبلغ کل امروز: ${fmt(f.optDouble("todayInvoiceAmount"))}")
                    Text("سهم نیروها امروز: ${fmt(f.optDouble("todayEmployeeShare"))}")
                    Text("پرداختی امروز: ${fmt(f.optDouble("todayPaymentAmount"))}")
                }
            }
        }
        dashboard?.let { d ->
            Spacer(Modifier.height(8.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("سفارشات امروز: ${d.optInt("todayCount")}")
                    Text("منتظر چاپ: ${d.optInt("pendingPrintCount")}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("خلاصه‌ی کلی سفارشات خاش", style = MaterialTheme.typography.titleMedium)
        summary?.let { s ->
            Spacer(Modifier.height(8.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("کل: ${s.optInt("total")}")
                    Text("ارسال از خاش: ${s.optInt("sentFromKhash")}")
                    Text("دریافت از خاش: ${s.optInt("receivedFromKhash")}")
                    Text("چاپ‌شده: ${s.optInt("printedIranshahr")}")
                    Text("ارسال به خاش: ${s.optInt("sentBackToKhash")}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("گزارش بر اساس تاریخ", style = MaterialTheme.typography.titleMedium)
        byDate.forEach { g ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text(g.optString("date"), style = MaterialTheme.typography.titleSmall)
                    Text("کل: ${g.optInt("total")} — چاپ‌شده: ${g.optInt("printedIranshahr")} — ارسال به خاش: ${g.optInt("sentBackToKhash")}")
                }
            }
        }
    }
}
