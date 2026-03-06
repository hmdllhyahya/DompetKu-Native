package com.dompetku.ui.screen.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private data class CurrencyItem(val code: String, val name: String, val flag: String, val symbol: String = "")

private val QUICK_CURRENCIES = listOf(
    CurrencyItem("USD", "Dolar AS",            "🇺🇸", "$"),
    CurrencyItem("EUR", "Euro",                "🇪🇺", "€"),
    CurrencyItem("GBP", "Pound Sterling",      "🇬🇧", "£"),
    CurrencyItem("JPY", "Yen Jepang",          "🇯🇵", "¥"),
    CurrencyItem("SGD", "Dolar Singapura",     "🇸🇬", "S$"),
    CurrencyItem("MYR", "Ringgit Malaysia",    "🇲🇾", "RM"),
    CurrencyItem("AUD", "Dolar Australia",     "🇦🇺", "A$"),
    CurrencyItem("SAR", "Riyal Arab Saudi",    "🇸🇦", "﷼"),
    CurrencyItem("CNY", "Yuan Tiongkok",       "🇨🇳", "¥"),
    CurrencyItem("KRW", "Won Korea",           "🇰🇷", "₩"),
    CurrencyItem("INR", "Rupee India",         "🇮🇳", "₹"),
    CurrencyItem("PICK","Pilih Negara",        "🌏", "?"),
)

// Offline fallback rates: 1 XXX = Y IDR
private val FALLBACK_RATES = mapOf(
    "USD" to 15800.0, "EUR" to 17200.0, "GBP" to 20100.0, "JPY" to 105.0,
    "SGD" to 11900.0, "MYR" to 3550.0,  "AUD" to 10400.0, "SAR" to 4210.0,
    "CNY" to 2180.0,  "KRW" to 11.8,    "INR" to 190.0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySheet(totalBalance: Long, onDismiss: () -> Unit) {
    var selected     by remember { mutableStateOf("USD") }
    var rates        by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var loading      by remember { mutableStateOf(true) }
    var errMsg       by remember { mutableStateOf("") }
    var lastUpdate   by remember { mutableStateOf("") }
    var showPicker   by remember { mutableStateOf(false) }
    var pickSearch   by remember { mutableStateOf("") }
    var customCode   by remember { mutableStateOf<String?>(null) }

    // Fetch rates on first composition
    LaunchedEffect(Unit) {
        loading = true; errMsg = ""
        try {
            val json = withContext(Dispatchers.IO) {
                URL("https://api.exchangerate-api.com/v4/latest/USD").readText()
            }
            val obj = JSONObject(json).getJSONObject("rates")
            val idrPerUsd = obj.optDouble("IDR", 15800.0)
            val result = mutableMapOf<String, Double>("IDR" to 1.0)
            obj.keys().forEach { code ->
                val rate = obj.optDouble(code, 0.0)
                if (rate > 0) result[code] = idrPerUsd / rate
            }
            rates = result
            lastUpdate = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id")).format(java.util.Date())
        } catch (e: Exception) {
            rates = FALLBACK_RATES
            errMsg = "Menggunakan kurs estimasi (offline)"
        } finally {
            loading = false
        }
    }

    val activeCurrencyCode = customCode ?: selected
    val activeCurrency = QUICK_CURRENCIES.find { it.code == activeCurrencyCode }
        ?: CurrencyItem(activeCurrencyCode, activeCurrencyCode, "🏳️")
    val idrPerUnit    = rates[activeCurrencyCode] ?: 0.0
    val totalForeign  = if (idrPerUnit > 0) totalBalance / idrPerUnit else null

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = PageBg,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            // Sheet title
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Text("Konversi Kurs", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5E7EB)),
                ) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(14.dp))
                }
            }

            // Balance card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(16.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column {
                    Text("Total Saldo", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(CurrencyFormatter.format(totalBalance), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        if (totalForeign != null && idrPerUnit > 0) {
                            Text(
                                text     = "  / ${activeCurrency.symbol}${formatForeign(totalForeign)} ${activeCurrency.code}",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color    = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Currency selector grid (4 per row)
            Text("PILIH MATA UANG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 10.dp))

            val rows = QUICK_CURRENCIES.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    rowItems.forEach { cur ->
                        val isActive = if (cur.code == "PICK") customCode != null else (customCode ?: selected) == cur.code
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) GreenLight else Color(0xFFF8FAFC))
                                .clickable {
                                    if (cur.code == "PICK") { showPicker = true }
                                    else { selected = cur.code; customCode = null }
                                }
                                .padding(8.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(cur.flag, fontSize = 20.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text       = if (cur.code == "PICK") (customCode ?: "Pilih") else cur.code,
                                    fontSize   = 11.sp, fontWeight = FontWeight.Bold,
                                    color      = if (isActive) GreenPrimary else TextDark,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rate display card
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(22.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF111827))
                        .padding(16.dp),
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.Top,
                            modifier              = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                val metaFlag = CURRENCY_META[activeCurrencyCode]?.second ?: activeCurrency.flag
                                val metaName = CURRENCY_META[activeCurrencyCode]?.first ?: activeCurrency.name
                                Text("$metaFlag $metaName", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text       = if (idrPerUnit > 0) "Rp ${Math.round(idrPerUnit).toString().reversed().chunked(3).joinToString(".").reversed()}" else "-",
                                    color      = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black,
                                )
                                Text("per 1 ${activeCurrency.code}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                            TextButton(
                                onClick = { /* re-fetch by toggling loading */
                                    loading = true
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                            ) {
                                Icon(PhosphorIcons.Regular.ArrowClockwise, null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh", fontSize = 11.sp)
                            }
                        }
                        if (lastUpdate.isNotEmpty()) {
                            Text("Update: $lastUpdate", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (errMsg.isNotEmpty()) {
                            Text(errMsg, color = Color(0xFFFCA5A5), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Country picker overlay
        if (showPicker) {
            CountryPickerSheet(
                rates      = rates,
                search     = pickSearch,
                onSearch   = { pickSearch = it },
                onSelect   = { code -> customCode = code; showPicker = false; pickSearch = "" },
                onDismiss  = { showPicker = false },
            )
        }
    }
}

// ── Currency name + flag lookup ───────────────────────────────────────────────
private val CURRENCY_META: Map<String, Pair<String, String>> = mapOf(
    "AED" to ("Dirham UAE" to "🇦🇪"), "AFN" to ("Afghani" to "🇦🇫"), "ALL" to ("Lek Albania" to "🇦🇱"),
    "AMD" to ("Dram Armenia" to "🇦🇲"), "ANG" to ("Guilder Antilles" to "🇳🇱"), "AOA" to ("Kwanza Angola" to "🇦🇴"),
    "ARS" to ("Peso Argentina" to "🇦🇷"), "AUD" to ("Dolar Australia" to "🇦🇺"), "AWG" to ("Florin Aruba" to "🇦🇼"),
    "AZN" to ("Manat Azerbaijan" to "🇦🇿"), "BAM" to ("Mark Bosnia" to "🇧🇦"), "BBD" to ("Dolar Barbados" to "🇧🇧"),
    "BDT" to ("Taka Bangladesh" to "🇧🇩"), "BGN" to ("Lev Bulgaria" to "🇧🇬"), "BHD" to ("Dinar Bahrain" to "🇧🇭"),
    "BIF" to ("Franc Burundi" to "🇧🇮"), "BMD" to ("Dolar Bermuda" to "🇧🇲"), "BND" to ("Dolar Brunei" to "🇧🇳"),
    "BOB" to ("Boliviano Bolivia" to "🇧🇴"), "BRL" to ("Real Brasil" to "🇧🇷"), "BSD" to ("Dolar Bahamas" to "🇧🇸"),
    "BTN" to ("Ngultrum Bhutan" to "🇧🇹"), "BWP" to ("Pula Botswana" to "🇧🇼"), "BYN" to ("Rubel Belarus" to "🇧🇾"),
    "BZD" to ("Dolar Belize" to "🇧🇿"), "CAD" to ("Dolar Kanada" to "🇨🇦"), "CDF" to ("Franc Kongo" to "🇨🇩"),
    "CHF" to ("Franc Swiss" to "🇨🇭"), "CLP" to ("Peso Chili" to "🇨🇱"), "CNY" to ("Yuan Tiongkok" to "🇨🇳"),
    "COP" to ("Peso Kolombia" to "🇨🇴"), "CRC" to ("Colon Kosta Rika" to "🇨🇷"), "CUP" to ("Peso Kuba" to "🇨🇺"),
    "CVE" to ("Escudo Tanjung Verde" to "🇨🇻"), "CZK" to ("Koruna Ceko" to "🇨🇿"), "DJF" to ("Franc Djibouti" to "🇩🇯"),
    "DKK" to ("Krone Denmark" to "🇩🇰"), "DOP" to ("Peso Dominika" to "🇩🇴"), "DZD" to ("Dinar Aljazair" to "🇩🇿"),
    "EGP" to ("Pound Mesir" to "🇪🇬"), "ERN" to ("Nakfa Eritrea" to "🇪🇷"), "ETB" to ("Birr Etiopia" to "🇪🇹"),
    "EUR" to ("Euro" to "🇪🇺"), "FJD" to ("Dolar Fiji" to "🇫🇯"), "FKP" to ("Pound Falkland" to "🇫🇰"),
    "GBP" to ("Pound Sterling" to "🇬🇧"), "GEL" to ("Lari Georgia" to "🇬🇪"), "GHS" to ("Cedi Ghana" to "🇬🇭"),
    "GIP" to ("Pound Gibraltar" to "🇬🇮"), "GMD" to ("Dalasi Gambia" to "🇬🇲"), "GNF" to ("Franc Guinea" to "🇬🇳"),
    "GTQ" to ("Quetzal Guatemala" to "🇬🇹"), "GYD" to ("Dolar Guyana" to "🇬🇾"), "HKD" to ("Dolar Hong Kong" to "🇭🇰"),
    "HNL" to ("Lempira Honduras" to "🇭🇳"), "HRK" to ("Kuna Kroasia" to "🇭🇷"), "HTG" to ("Gourde Haiti" to "🇭🇹"),
    "HUF" to ("Forint Hungaria" to "🇭🇺"), "IDR" to ("Rupiah Indonesia" to "🇮🇩"), "ILS" to ("Shekel Israel" to "🇮🇱"),
    "INR" to ("Rupee India" to "🇮🇳"), "IQD" to ("Dinar Irak" to "🇮🇶"), "IRR" to ("Rial Iran" to "🇮🇷"),
    "ISK" to ("Krona Islandia" to "🇮🇸"), "JMD" to ("Dolar Jamaika" to "🇯🇲"), "JOD" to ("Dinar Yordania" to "🇯🇴"),
    "JPY" to ("Yen Jepang" to "🇯🇵"), "KES" to ("Shilling Kenya" to "🇰🇪"), "KGS" to ("Som Kirgistan" to "🇰🇬"),
    "KHR" to ("Riel Kamboja" to "🇰🇭"), "KMF" to ("Franc Komoro" to "🇰🇲"), "KPW" to ("Won Korea Utara" to "🇰🇵"),
    "KRW" to ("Won Korea" to "🇰🇷"), "KWD" to ("Dinar Kuwait" to "🇰🇼"), "KYD" to ("Dolar Cayman" to "🇰🇾"),
    "KZT" to ("Tenge Kazakhstan" to "🇰🇿"), "LAK" to ("Kip Laos" to "🇱🇦"), "LBP" to ("Pound Lebanon" to "🇱🇧"),
    "LKR" to ("Rupee Sri Lanka" to "🇱🇰"), "LRD" to ("Dolar Liberia" to "🇱🇷"), "LSL" to ("Loti Lesotho" to "🇱🇸"),
    "LYD" to ("Dinar Libya" to "🇱🇾"), "MAD" to ("Dirham Maroko" to "🇲🇦"), "MDL" to ("Leu Moldova" to "🇲🇩"),
    "MGA" to ("Ariary Madagaskar" to "🇲🇬"), "MKD" to ("Denar Makedonia" to "🇲🇰"), "MMK" to ("Kyat Myanmar" to "🇲🇲"),
    "MNT" to ("Tugrik Mongolia" to "🇲🇳"), "MOP" to ("Pataca Makau" to "🇲🇴"), "MRU" to ("Ouguiya Mauritania" to "🇲🇷"),
    "MUR" to ("Rupee Mauritius" to "🇲🇺"), "MVR" to ("Rufiyaa Maladewa" to "🇲🇻"), "MWK" to ("Kwacha Malawi" to "🇲🇼"),
    "MXN" to ("Peso Meksiko" to "🇲🇽"), "MYR" to ("Ringgit Malaysia" to "🇲🇾"), "MZN" to ("Metical Mozambik" to "🇲🇿"),
    "NAD" to ("Dolar Namibia" to "🇳🇦"), "NGN" to ("Naira Nigeria" to "🇳🇬"), "NIO" to ("Cordoba Nikaragua" to "🇳🇮"),
    "NOK" to ("Krone Norwegia" to "🇳🇴"), "NPR" to ("Rupee Nepal" to "🇳🇵"), "NZD" to ("Dolar Selandia Baru" to "🇳🇿"),
    "OMR" to ("Rial Oman" to "🇴🇲"), "PAB" to ("Balboa Panama" to "🇵🇦"), "PEN" to ("Sol Peru" to "🇵🇪"),
    "PGK" to ("Kina Papua Nugini" to "🇵🇬"), "PHP" to ("Peso Filipina" to "🇵🇭"), "PKR" to ("Rupee Pakistan" to "🇵🇰"),
    "PLN" to ("Zloty Polandia" to "🇵🇱"), "PYG" to ("Guarani Paraguay" to "🇵🇾"), "QAR" to ("Riyal Qatar" to "🇶🇦"),
    "RON" to ("Leu Rumania" to "🇷🇴"), "RSD" to ("Dinar Serbia" to "🇷🇸"), "RUB" to ("Rubel Rusia" to "🇷🇺"),
    "RWF" to ("Franc Rwanda" to "🇷🇼"), "SAR" to ("Riyal Arab Saudi" to "🇸🇦"), "SBD" to ("Dolar Solomon" to "🇸🇧"),
    "SCR" to ("Rupee Seychelles" to "🇸🇨"), "SDG" to ("Pound Sudan" to "🇸🇩"), "SEK" to ("Krona Swedia" to "🇸🇪"),
    "SGD" to ("Dolar Singapura" to "🇸🇬"), "SHP" to ("Pound St. Helena" to "🇸🇭"), "SLE" to ("Leone Sierra Leone" to "🇸🇱"),
    "SOS" to ("Shilling Somalia" to "🇸🇴"), "SRD" to ("Dolar Suriname" to "🇸🇷"), "STN" to ("Dobra Sao Tome" to "🇸🇹"),
    "SVC" to ("Colon El Salvador" to "🇸🇻"), "SYP" to ("Pound Suriah" to "🇸🇾"), "SZL" to ("Lilangeni Eswatini" to "🇸🇿"),
    "THB" to ("Baht Thailand" to "🇹🇭"), "TJS" to ("Somoni Tajikistan" to "🇹🇯"), "TMT" to ("Manat Turkmenistan" to "🇹🇲"),
    "TND" to ("Dinar Tunisia" to "🇹🇳"), "TOP" to ("Pa'anga Tonga" to "🇹🇴"), "TRY" to ("Lira Turki" to "🇹🇷"),
    "TTD" to ("Dolar Trinidad" to "🇹🇹"), "TWD" to ("Dolar Taiwan" to "🇹🇼"), "TZS" to ("Shilling Tanzania" to "🇹🇿"),
    "UAH" to ("Hryvnia Ukraina" to "🇺🇦"), "UGX" to ("Shilling Uganda" to "🇺🇬"), "USD" to ("Dolar AS" to "🇺🇸"),
    "UYU" to ("Peso Uruguay" to "🇺🇾"), "UZS" to ("Som Uzbekistan" to "🇺🇿"), "VES" to ("Bolivar Venezuela" to "🇻🇪"),
    "VND" to ("Dong Vietnam" to "🇻🇳"), "VUV" to ("Vatu Vanuatu" to "🇻🇺"), "WST" to ("Tala Samoa" to "🇼🇸"),
    "XAF" to ("Franc CFA Afrika Tengah" to "🌍"), "XCD" to ("Dolar Karibia" to "🌎"),
    "XOF" to ("Franc CFA Afrika Barat" to "🌍"), "XPF" to ("Franc CFP" to "🌏"),
    "YER" to ("Rial Yaman" to "🇾🇪"), "ZAR" to ("Rand Afrika Selatan" to "🇿🇦"),
    "ZMW" to ("Kwacha Zambia" to "🇿🇲"), "ZWL" to ("Dolar Zimbabwe" to "🇿🇼"),
)

// ── Country picker ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    rates:    Map<String, Double>,
    search:   String,
    onSearch: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = CardWhite,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text("Pilih Negara", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(15.dp))
                }
            }
            OutlinedTextField(
                value         = search,
                onValueChange = onSearch,
                placeholder   = { Text("Cari negara atau kode mata uang...") },
                leadingIcon   = { Icon(PhosphorIcons.Regular.MagnifyingGlass, null, modifier = Modifier.size(16.dp)) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                val filteredCodes = rates.keys.filter { code ->
                    search.isEmpty() ||
                    code.contains(search, ignoreCase = true)
                }.sorted()
                items(filteredCodes) { code ->
                    val rate = rates[code] ?: 0.0
                    val meta = CURRENCY_META[code]
                    val flag = meta?.second ?: "🏳️"
                    val name = meta?.first ?: code
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(flag, fontSize = 22.sp, modifier = androidx.compose.ui.Modifier.padding(end = 12.dp))
                            Column {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Text(code, fontSize = 10.sp, color = TextLight)
                            }
                        }
                        if (rate > 0) {
                            Text(
                                text     = "Rp${Math.round(rate).toString().reversed().chunked(3).joinToString(".").reversed()}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary,
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun formatForeign(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.2fM", value / 1_000_000_000)
        value >= 1_000_000     -> String.format("%.2fJt", value / 1_000_000)
        value >= 1_000         -> String.format("%.1fK", value / 1_000)
        value < 1              -> String.format("%.4f", value)
        else                   -> String.format("%.2f", value)
    }
}
