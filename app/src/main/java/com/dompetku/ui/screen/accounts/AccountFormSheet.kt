package com.dompetku.ui.screen.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.ui.components.GreenButton
import com.dompetku.ui.theme.*
import com.dompetku.util.BrandDetector
import com.dompetku.util.CurrencyFormatter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.*

// ── Account type items with icons ─────────────────────────────────────────────
private data class AccTypeItem(val type: AccountType, val label: String, val icon: ImageVector)
private val ACC_TYPES = listOf(
    AccTypeItem(AccountType.cash,       "Dompet / Tunai",        PhosphorIcons.Regular.Wallet),
    AccTypeItem(AccountType.bank,       "Rekening Debit",        PhosphorIcons.Regular.CreditCard),
    AccTypeItem(AccountType.credit,     "Kartu Kredit",          PhosphorIcons.Regular.CreditCard),
    AccTypeItem(AccountType.ewallet,    "E-Wallet",              PhosphorIcons.Regular.DeviceMobile),
    AccTypeItem(AccountType.emoney,     "Kartu E-Money (KUE)",   PhosphorIcons.Regular.WifiHigh),
    AccTypeItem(AccountType.savings,    "Tabungan",              PhosphorIcons.Regular.PiggyBank),
    AccTypeItem(AccountType.investment, "Investasi",             PhosphorIcons.Regular.TrendUp),
)

private val BANKS    = listOf("BCA","BRI","BNI","Mandiri","BSI","CIMB Niaga","Permata","Danamon","Panin","OCBC","Maybank","BTN","Bukopin","Muamalat","BJB","Jago","Jago Syariah","Jenius","SeaBank","Blu","HSBC","Citibank","Lainnya")
private val EWALLETS = listOf("GoPay","OVO","DANA","ShopeePay","LinkAja","Flip","Lainnya")
private val EMONEY   = listOf("Flazz (BCA)","e-Money (Mandiri)","TapCash (BNI)","Brizzi (BRI)","JakCard (Bank DKI)","Nobu e-Money","Mega Cash","BSI e-Money","BRIZZI","Lainnya")

private val GRADIENT_PRESETS = listOf(
    Color(0xFF2DAB7F) to Color(0xFF1D8A63),
    Color(0xFF1D4ED8) to Color(0xFF60A5FA),
    Color(0xFF7C3AED) to Color(0xFFC084FC),
    Color(0xFFEF4444) to Color(0xFFF97316),
    Color(0xFF0EA5E9) to Color(0xFF6366F1),
    Color(0xFFEC4899) to Color(0xFFF97316),
    Color(0xFF374151) to Color(0xFF6B7280),
    Color(0xFFB45309) to Color(0xFFF59E0B),
    Color(0xFF0F766E) to Color(0xFF06B6D4),
    Color(0xFFBE185D) to Color(0xFFF43F5E),
    Color(0xFF1E40AF) to Color(0xFF7C3AED),
    Color(0xFF065F46) to Color(0xFF059669),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    initial:   Account? = null,
    onDismiss: () -> Unit,
    onSave:    (account: Account, balanceAdjustment: Long) -> Unit,
) {
    val isNew = initial == null

    var accType    by remember { mutableStateOf(initial?.type ?: AccountType.bank) }
    var name       by remember { mutableStateOf(initial?.name ?: "") }
    var customName by remember { mutableStateOf("") }  // shown when "Lainnya" selected
    var isCustom   by remember { mutableStateOf(false) }
    var last4      by remember { mutableStateOf(initial?.last4 ?: "") }
    var balanceStr by remember { mutableStateOf(if (isNew) "" else initial!!.balance.toString()) }
    var adjStr     by remember { mutableStateOf(if (!isNew) initial!!.balance.toString() else "") }

    var gradStart      by remember { mutableStateOf(if (initial != null) Color(initial.gradientStart.toInt()) else GreenPrimary) }
    var gradEnd        by remember { mutableStateOf(if (initial != null) Color(initial.gradientEnd.toInt())   else GreenDark) }
    var showGradPicker by remember { mutableStateOf(false) }
    var editingStart   by remember { mutableStateOf(true) }

    val brandInfo = remember(name) { BrandDetector.detect(name) }
    LaunchedEffect(brandInfo) {
        if (brandInfo != null) {
            gradStart = brandInfo.gradientStart
            gradEnd   = brandInfo.gradientEnd
        }
    }

    val opts = when (accType) {
        AccountType.bank, AccountType.credit, AccountType.savings -> BANKS
        AccountType.ewallet -> EWALLETS
        AccountType.emoney  -> EMONEY
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            // ── Header ────────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
            ) {
                Text(
                    text       = if (isNew) "Tambah Akun" else "Edit Akun",
                    fontSize   = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark,
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5E7EB)),
                ) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(14.dp))
                }
            }

            // ── Account type — 2-column grid with icons ───────────────────────
            val rows = ACC_TYPES.chunked(2)
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    row.forEach { item ->
                        val active = accType == item.type
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardWhite)
                                .border(
                                    width = if (active) 1.5.dp else 0.dp,
                                    color = if (active) GreenPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { accType = item.type; name = "" }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = null,
                                tint               = if (active) GreenPrimary else TextMedium,
                                modifier           = Modifier.size(16.dp),
                            )
                            Text(
                                text       = item.label,
                                fontSize   = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                color      = if (active) GreenPrimary else TextMedium,
                            )
                        }
                    }
                    // fill last row if odd
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Name / selector ───────────────────────────────────────────────
            val fieldLabel = when (accType) {
                AccountType.ewallet -> "E-WALLET"
                AccountType.emoney  -> "KARTU E-MONEY"
                AccountType.cash    -> "NAMA DOMPET"
                else                -> "NAMA BANK / AKUN"
            }
            FormField(label = fieldLabel) {
                if (opts != null && !isCustom) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value         = name.ifEmpty { "-- Pilih --" },
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            colors        = transparentFieldColors(),
                            modifier      = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardWhite)) {
                            opts.forEach { opt ->
                                DropdownMenuItem(
                                    text    = { Text(opt, fontSize = 14.sp) },
                                    onClick = {
                                        if (opt == "Lainnya") {
                                            isCustom = true
                                            name = customName
                                        } else {
                                            name = opt
                                            isCustom = false
                                        }
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                } else {
                    // Free-text input (either non-list type, or "Lainnya" selected)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value         = if (isCustom) customName else name,
                            onValueChange = {
                                if (isCustom) { customName = it; name = it }
                                else name = it
                            },
                            placeholder   = { Text(
                                when {
                                    isCustom && accType == AccountType.ewallet -> "cth: Dana, QRIS..."
                                    isCustom -> "cth: Bank Jago Syariah..."
                                    accType == AccountType.cash -> "cth: Dompet Harian"
                                    else -> "cth: Nama akun..."
                                }, color = TextLight
                            )},
                            colors        = transparentFieldColors(),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                        )
                        if (isCustom) {
                            TextButton(onClick = { isCustom = false; customName = ""; name = "" }) {
                                Text("Pilih list", fontSize = 11.sp, color = GreenPrimary)
                            }
                        }
                    }
                }
            }

            // Brand detected badge
            if (brandInfo != null) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(GreenLight)
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    Icon(PhosphorIcons.Regular.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(13.dp))
                    Text("Brand ${brandInfo.displayName} terdeteksi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                }
            }

            // ── 4-digit last ──────────────────────────────────────────────────
            FormField(label = "4 DIGIT TERAKHIR (OPSIONAL)") {
                OutlinedTextField(
                    value           = last4,
                    onValueChange   = { if (it.length <= 4 && it.all(Char::isDigit)) last4 = it },
                    placeholder     = { Text("xxxx", color = TextLight) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors          = transparentFieldColors(),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                )
            }

            // ── Card preview ──────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            ) {
                Text("TAMPILAN KARTU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.5.sp)
                TextButton(onClick = { showGradPicker = !showGradPicker }) {
                    Text(if (showGradPicker) "Tutup" else "Kustomisasi", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            CardPreview(name = name, last4 = last4, accType = accType, gradStart = gradStart, gradEnd = gradEnd)

            if (showGradPicker) {
                ColorPickerPanel(
                    gradStart    = gradStart,
                    gradEnd      = gradEnd,
                    editingStart = editingStart,
                    onSelectStart = { editingStart = true },
                    onSelectEnd   = { editingStart = false },
                    onColorChange = { c -> if (editingStart) gradStart = c else gradEnd = c },
                    presets       = GRADIENT_PRESETS,
                    onPresetClick = { s, e -> gradStart = s; gradEnd = e },
                )
            }

            // ── Balance ───────────────────────────────────────────────────────
            if (isNew) {
                FormField(label = "SALDO AWAL (RP)") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = if (balanceStr.isEmpty()) "" else balanceStr.toLongOrNull()?.let {
                                java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).format(it)
                            } ?: balanceStr,
                            onValueChange = { raw ->
                                val digits = raw.replace("[^0-9]".toRegex(), "")
                                balanceStr = if (digits.isEmpty()) "" else digits
                            },
                            placeholder     = { Text("0", color = TextLight) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle       = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDark),
                            colors          = transparentFieldColors(),
                            singleLine      = true,
                            modifier        = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                FormField(label = "SESUAIKAN SALDO") {
                    Text("Saldo saat ini: ${CurrencyFormatter.format(initial!!.balance)}", fontSize = 11.sp, color = TextMedium, modifier = Modifier.padding(bottom = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = if (adjStr.isEmpty()) "" else adjStr.toLongOrNull()?.let {
                                java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).format(it)
                            } ?: adjStr,
                            onValueChange   = { raw -> adjStr = raw.replace("[^0-9]".toRegex(), "") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle       = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextDark),
                            colors          = transparentFieldColors(),
                            singleLine      = true,
                            modifier        = Modifier.weight(1f),
                        )
                    }
                    val newBal = adjStr.toLongOrNull() ?: 0L
                    val diff   = newBal - initial!!.balance
                    if (diff != 0L) {
                        val pos = diff > 0
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (pos) GreenLight else RedLight)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Icon(
                                if (pos) PhosphorIcons.Regular.ArrowUp else PhosphorIcons.Regular.ArrowDown,
                                null, tint = if (pos) GreenPrimary else RedExpense, modifier = Modifier.size(12.dp),
                            )
                            Text(
                                "${if (pos) "+" else ""}${CurrencyFormatter.format(diff)} — dicatat sbg Penyesuaian Saldo",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color    = if (pos) GreenPrimary else RedExpense,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            GreenButton(
                text    = if (isNew) "Tambah Akun" else "Simpan Perubahan",
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    val balLong    = if (isNew) (balanceStr.toLongOrNull() ?: 0L) else (adjStr.toLongOrNull() ?: 0L)
                    val adjustment = if (!isNew) balLong - initial!!.balance else 0L
                    onSave(
                        Account(
                            id            = initial?.id ?: "",
                            type          = accType,
                            name          = name.trim(),
                            balance       = if (isNew) balLong else (initial!!.balance + adjustment),
                            last4         = last4.takeIf { it.isNotEmpty() },
                            gradientStart = gradStart.toArgb().toLong(),
                            gradientEnd   = gradEnd.toArgb().toLong(),
                            brandKey      = brandInfo?.key,
                            sortOrder     = initial?.sortOrder ?: 0,
                        ),
                        adjustment,
                    )
                    onDismiss()
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Card preview ──────────────────────────────────────────────────────────────
@Composable
private fun CardPreview(name: String, last4: String?, accType: AccountType, gradStart: Color, gradEnd: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.2f)),
            ) {
                Icon(
                    imageVector = when (accType) {
                        AccountType.cash    -> PhosphorIcons.Regular.Wallet
                        AccountType.ewallet -> PhosphorIcons.Regular.DeviceMobile
                        AccountType.savings -> PhosphorIcons.Regular.PiggyBank
                        AccountType.emoney  -> PhosphorIcons.Regular.WifiHigh
                        else                -> PhosphorIcons.Regular.CreditCard
                    },
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Column {
                Text(name.ifEmpty { "Nama Akun" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (!last4.isNullOrEmpty()) "•••• $last4" else accType.name, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
private fun transparentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = GreenPrimary.copy(alpha = 0.4f),
    unfocusedBorderColor    = Color.Transparent,
    focusedContainerColor   = CardWhite,
    unfocusedContainerColor = CardWhite,
)

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 3.dp))
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR WHEEL PICKER COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ColorPickerPanel(
    gradStart:    Color,
    gradEnd:      Color,
    editingStart: Boolean,
    onSelectStart: () -> Unit,
    onSelectEnd:   () -> Unit,
    onColorChange: (Color) -> Unit,
    presets:       List<Pair<Color, Color>>,
    onPresetClick: (Color, Color) -> Unit,
) {
    val activeColor = if (editingStart) gradStart else gradEnd

    // Sync brightness from the active color whenever we switch which color we're editing
    var brightness by remember(editingStart) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(activeColor.toArgb(), hsv)
        mutableStateOf(hsv[2])
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .padding(bottom = 4.dp),
    ) {
        // ── Which gradient stop to edit ───────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            GradColorTab(
                label   = "Warna Mulai",
                color   = gradStart,
                active  = editingStart,
                onClick = onSelectStart,
                modifier = Modifier.weight(1f),
            )
            GradColorTab(
                label   = "Warna Akhir",
                color   = gradEnd,
                active  = !editingStart,
                onClick = onSelectEnd,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Color wheel ───────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            ColorWheel(
                color      = activeColor,
                brightness = brightness,
                onColorChange = { newColor ->
                    // Keep brightness from slider, update hue+sat
                    onColorChange(newColor)
                },
                modifier = Modifier.size(220.dp),
            )
        }

        // ── Brightness slider ─────────────────────────────────────────────────
        Text(
            "KECERAHAN",
            fontSize     = 10.sp, fontWeight = FontWeight.Bold,
            color        = TextLight, letterSpacing = 0.5.sp,
            modifier     = Modifier.padding(bottom = 6.dp),
        )
        BrightnessSlider(
            color      = activeColor,
            brightness = brightness,
            onBrightnessChange = { b ->
                brightness = b
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(activeColor.toArgb(), hsv)
                hsv[2] = b
                onColorChange(Color(android.graphics.Color.HSVToColor(hsv)))
            },
        )

        Spacer(Modifier.height(14.dp))

        // ── Quick presets ─────────────────────────────────────────────────────
        Text(
            "PRESET CEPAT",
            fontSize     = 10.sp, fontWeight = FontWeight.Bold,
            color        = TextLight, letterSpacing = 0.5.sp,
            modifier     = Modifier.padding(bottom = 6.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.padding(bottom = 4.dp),
        ) {
            itemsIndexed(presets) { _, pair ->
                val active = gradStart == pair.first && gradEnd == pair.second
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.linearGradient(listOf(pair.first, pair.second)))
                        .then(if (active) Modifier.border(2.dp, Color.White, RoundedCornerShape(9.dp)) else Modifier)
                        .clickable { onPresetClick(pair.first, pair.second) },
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ── Color wheel (HSV circular picker) ────────────────────────────────────────
@Composable
private fun ColorWheel(
    color:         Color,
    brightness:    Float,
    onColorChange: (Color) -> Unit,
    modifier:      Modifier = Modifier,
) {
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }

    var radiusPx by remember { mutableStateOf(0f) }

    fun updateFromOffset(offset: androidx.compose.ui.geometry.Offset) {
        if (radiusPx == 0f) return
        val dx  = offset.x - radiusPx
        val dy  = offset.y - radiusPx
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= radiusPx) {
            val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
            val sat = (dist / radiusPx).coerceIn(0f, 1f)
            onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, brightness))))
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { radiusPx = it.width / 2f }
            .pointerInput(brightness) {
                detectTapGestures { offset -> updateFromOffset(offset) }
            }
            .pointerInput(brightness) {
                detectDragGestures(
                    onDragStart = { offset -> updateFromOffset(offset) },
                    onDrag      = { change, _ -> updateFromOffset(change.position) },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val cx     = size.width  / 2f
            val cy     = size.height / 2f
            val center = androidx.compose.ui.geometry.Offset(cx, cy)

            // 1. Hue sweep — ShaderBrush wraps android SweepGradient, no nativeCanvas needed
            val hueColors = IntArray(361) { i ->
                android.graphics.Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
            }
            drawCircle(
                brush  = ShaderBrush(android.graphics.SweepGradient(cx, cy, hueColors, null)),
                radius = radius,
                center = center,
            )

            // 2. Saturation radial overlay (white center → transparent rim)
            drawCircle(
                brush = ShaderBrush(
                    android.graphics.RadialGradient(
                        cx, cy, radius,
                        intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
                        null,
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                ),
                radius = radius,
                center = center,
            )

            // 3. Brightness overlay (black with varying alpha)
            val brightAlpha = ((1f - brightness) * 255).toInt().coerceIn(0, 255)
            if (brightAlpha > 0) {
                drawCircle(
                    color  = Color.Black.copy(alpha = brightAlpha / 255f),
                    radius = radius,
                    center = center,
                )
            }

            // 4. Selector dot — position from current HSV
            val selectorAngle = Math.toRadians(hsv[0].toDouble())
            val selectorDist  = hsv[1] * radius
            val sx = (cx + cos(selectorAngle) * selectorDist).toFloat()
            val sy = (cy + sin(selectorAngle) * selectorDist).toFloat()
            val dotCenter = androidx.compose.ui.geometry.Offset(sx, sy)

            drawCircle(Color.White,  radius = 11.dp.toPx(), center = dotCenter)
            drawCircle(color,        radius =  8.dp.toPx(), center = dotCenter)
            drawCircle(
                color  = Color.Black.copy(alpha = 0.15f),
                radius = 11.dp.toPx(),
                center = dotCenter,
                style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

// ── Brightness slider ─────────────────────────────────────────────────────────
@Composable
private fun BrightnessSlider(
    color:              Color,
    brightness:         Float,
    onBrightnessChange: (Float) -> Unit,
) {
    var widthPx by remember { mutableStateOf(0) }

    // Full-bright version of the color for the gradient end stop
    val fullColor = remember(color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[2] = 1f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .onSizeChanged { widthPx = it.width }
            .background(Brush.horizontalGradient(listOf(Color.Black, fullColor)))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onBrightnessChange((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onBrightnessChange((offset.x / widthPx).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        onBrightnessChange((change.position.x / widthPx).coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        if (widthPx > 0) {
            val thumbX = (brightness * widthPx).coerceIn(14f, widthPx - 14f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbX.toInt() - 14, 0) }
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFFDDE0E4), CircleShape),
            )
        }
    }
}

// ── Gradient color tab (Warna Mulai / Warna Akhir toggle) ─────────────────────
@Composable
private fun GradColorTab(
    label:    String,
    color:    Color,
    active:   Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) GreenLight else Color(0xFFF3F4F6))
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) GreenPrimary else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
                .border(1.dp, Color(0x22000000), RoundedCornerShape(5.dp)),
        )
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            color      = if (active) GreenPrimary else TextMedium,
        )
    }
}
