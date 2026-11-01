package com.rajaanda.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF18352D)
private val Green = Color(0xFF246B50)
private val Cream = Color(0xFFF5F7F2)
private val Gold = Color(0xFFE4B65B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Cream, surface = Color.White)) { RajaApp() } }
    }
}

data class Sale(val kind: String, val quantity: Int, val amount: Double, val time: String)

@Composable
fun RajaApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("raja_store", 0) }
    var stock by remember { mutableIntStateOf(prefs.getInt("stock_trays", 0)) }
    var wholesale by remember { mutableStateOf(prefs.getString("wholesale_price", "200") ?: "200") }
    var retail by remember { mutableStateOf(prefs.getString("retail_price", "8") ?: "8") }
    var purchase by remember { mutableStateOf(prefs.getString("purchase_price", "180") ?: "180") }
    var sales by remember { mutableStateOf(loadSales(prefs.getString("sales", "[]") ?: "[]")) }
    var tab by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    fun save() {
        prefs.edit().putInt("stock_trays", stock).putString("wholesale_price", wholesale)
            .putString("retail_price", retail).putString("purchase_price", purchase)
            .putString("sales", salesJson(sales)).apply()
    }
    val soldTrays = sales.filter { it.kind == "Wholesale" }.sumOf { it.quantity } +
        sales.filter { it.kind == "Retail" }.sumOf { it.quantity } / 30
    val revenue = sales.sumOf { it.amount }

    Scaffold(
        containerColor = Cream,
        topBar = {
            Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RAJA ANDA", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text("STORE MANAGER  •  HARDI, BAHRAICH", color = Color(0xFFB8D2C6), style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.Egg, contentDescription = null, tint = Gold, modifier = Modifier.size(30.dp))
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Overview") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("Stock") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.PointOfSale, null) }, label = { Text("Sales") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Prices") })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (tab) {
                0 -> {
                    Text("Good day 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Here is how your shop is doing.", color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("Available stock", "$stock trays", "≈ ${stock * 30} eggs", Modifier.weight(1f))
                        MetricCard("Sales recorded", "₹${money(revenue)}", "${sales.size} transactions", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("Wholesale price", "₹$wholesale", "per tray (30 eggs)", Modifier.weight(1f))
                        MetricCard("Retail price", "₹$retail", "per egg", Modifier.weight(1f))
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("QUICK ACTIONS", color = Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { tab = 2; dialog = "Wholesale" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Wholesale sale") }
                                OutlinedButton(onClick = { tab = 2; dialog = "Retail" }, modifier = Modifier.weight(1f)) { Text("Retail sale") }
                            }
                            OutlinedButton(onClick = { tab = 1; dialog = "Stock" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("＋  Add stock received") }
                        }
                    }
                    Text("Recent activity", fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleMedium)
                    if (sales.isEmpty()) Text("No sales recorded yet. Your transactions will appear here.", color = Color.Gray)
                    sales.takeLast(5).reversed().forEach { SaleRow(it) }
                    Text("Note: sales and stock are stored on this phone. Back up your data regularly.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                1 -> {
                    Text("Inventory", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                    MetricCard("Current stock", "$stock trays", "${stock * 30} eggs • ${stock / 7} bundles + ${stock % 7} trays", Modifier.fillMaxWidth())
                    Button(onClick = { qty = "1"; dialog = "Stock" }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Record stock received") }
                    OutlinedButton(onClick = { qty = "1"; dialog = "Remove" }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Remove, null); Spacer(Modifier.width(8.dp)); Text("Adjust stock down") }
                    Text("1 tray = 30 eggs  •  1 bundle = 7 trays", color = Color.Gray)
                }
                2 -> {
                    Text("Sales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { qty = "1"; dialog = "Wholesale" }, modifier = Modifier.weight(1f)) { Text("Wholesale") }
                        OutlinedButton(onClick = { qty = "30"; dialog = "Retail" }, modifier = Modifier.weight(1f)) { Text("Retail") }
                    }
                    Text("Recorded transactions", fontWeight = FontWeight.Bold, color = Ink)
                    if (sales.isEmpty()) Text("No transactions yet.")
                    sales.reversed().forEach { SaleRow(it) }
                }
                3 -> {
                    Text("Price settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                    PriceField("Wholesale selling price / tray (₹)", wholesale) { wholesale = it; save() }
                    PriceField("Retail selling price / egg (₹)", retail) { retail = it; save() }
                    PriceField("Purchase cost / tray (₹)", purchase) { purchase = it; save() }
                    Text("Prices are editable and are not fixed by the app.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (dialog.isNotEmpty()) {
        val isSale = dialog == "Wholesale" || dialog == "Retail"
        val isRetail = dialog == "Retail"
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(when (dialog) { "Stock" -> "Add received stock"; "Remove" -> "Reduce stock"; "Wholesale" -> "Record wholesale sale"; else -> "Record retail sale" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it.filter(Char::isDigit) }, label = { Text(if (isRetail) "Eggs sold" else "Trays") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    if (isSale) {
                        val q = qty.toIntOrNull() ?: 0
                        val rate = (if (isRetail) retail else wholesale).toDoubleOrNull() ?: 0.0
                        Text("Sale total: ₹${money(q * rate)}", fontWeight = FontWeight.Bold)
                    } else {
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Reason / note (optional)") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val q = qty.toIntOrNull() ?: 0
                    if (q > 0) {
                        when (dialog) {
                            "Stock" -> stock += q
                            "Remove" -> stock = (stock - q).coerceAtLeast(0)
                            "Wholesale" -> if (q <= stock) { stock -= q; sales = sales + Sale("Wholesale", q, q * (wholesale.toDoubleOrNull() ?: 0.0), now()) }
                            "Retail" -> if (q <= stock * 30) { stock -= (q + 29) / 30; sales = sales + Sale("Retail", q, q * (retail.toDoubleOrNull() ?: 0.0), now()) }
                        }
                        save()
                    }
                    dialog = ""; note = ""
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Text(value, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Text(detail, color = Green, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PriceField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
}

@Composable
fun SaleRow(sale: Sale) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${sale.kind} sale", fontWeight = FontWeight.SemiBold, color = Ink)
                Text("${sale.quantity} ${if (sale.kind == "Retail") "eggs" else "trays"} • ${sale.time}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Text("₹${money(sale.amount)}", fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

fun now() = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
fun money(value: Double) = String.format(Locale.US, "%.2f", value)
fun loadSales(raw: String): List<Sale> = try {
    val a = JSONArray(raw)
    (0 until a.length()).map { val o = a.getJSONObject(it); Sale(o.getString("kind"), o.getInt("quantity"), o.getDouble("amount"), o.getString("time")) }
} catch (_: Exception) { emptyList() }
fun salesJson(sales: List<Sale>): String {
    val a = JSONArray()
    sales.forEach { a.put(JSONObject().put("kind", it.kind).put("quantity", it.quantity).put("amount", it.amount).put("time", it.time)) }
    return a.toString()
}
