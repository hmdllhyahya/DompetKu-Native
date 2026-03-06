package com.dompetku.ui.screen.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.ui.components.CategoryBubble
import com.dompetku.ui.components.GreenButton
import com.dompetku.ui.theme.*
import com.dompetku.util.DateUtils
import com.dompetku.util.SmartCategoryDetector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush

private val CATS_EXPENSE = listOf(
    "Makan & Minum","Belanja Harian","Belanja Online","Transportasi",
    "Hiburan","Tagihan","Kesehatan","Pendidikan","Tempat Tinggal",
    "Perawatan","Penyesuaian Saldo","Lainnya",
)
private val CATS_INCOME = listOf(
    "Gaji","Freelance","Hadiah","Investasi","Penyesuaian Saldo","Lainnya",
)

private data class TxnForm(
    val txType:    String  = "expense",
    val amountStr: String  = "",
    val category:  String  = "Makan & Minum",
    val note:      String  = "",
    val date:      String  = DateUtils.todayStr(),
    val time:      String  = DateUtils.nowTimeStr(),
    val accountId: String  = "",
    // Transfer fields
    val fromId:    String  = "",
    val toId:      String  = "",
    val adminFee:  String  = "",
    val detected:  Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    initial:   Transaction? = null,
    accounts:  List<Account>,
    onDismiss: () -> Unit,
    onSave:    (Transaction) -> Unit,
) {
    val isEdit = initial != null
    var form by remember {
        mutableStateOf(
            if (initial != null) TxnForm(
                txType    = initial.type.name,
                amountStr = initial.amount.toString(),
                category  = initial.category,
                note      = initial.note,
                date      = initial.date,
                time      = initial.time,
                accountId = initial.accountId,
                // If editing a non-transfer, pre-fill fromId with the source account
                // so switching to transfer type already has DARI AKUN selected
                fromId    = initial.fromId ?: initial.accountId,
                toId      = initial.toId   ?: "",
            )
            else TxnForm(accountId = accounts.firstOrNull()?.id ?: "",
                         fromId    = accounts.getOrNull(0)?.id ?: "",
                         toId      = accounts.getOrNull(1)?.id ?: "")
        )
    }

    var detectionBanner by remember { mutableStateOf<String?>(null) }

    fun handleNoteChange(note: String) {
        form = form.copy(note = note)
        val result = SmartCategoryDetector.detect(note)
        if (result != null && form.txType != "income") {
            form            = form.copy(category = result.category, detected = true)
            detectionBanner = result.category
        } else {
            detectionBanner = null
            form            = form.copy(detected = false)
        }
    }

    val cats = if (form.txType == "income") CATS_INCOME else CATS_EXPENSE

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = PageBg,
        shape             = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        windowInsets      = androidx.compose.foundation.layout.WindowInsets(0),
        modifier          = Modifier.fillMaxHeight(0.88f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            ) {
                Text(
                    text       = if (isEdit) "Edit Transaksi" else "Catat Transaksi",
                    fontSize   = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark,
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5E7EB)),
                ) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(14.dp))
                }
            }

            // ── Type segmented control with slide animation ────────────────────
            val tabs = listOf("expense" to "Pengeluaran", "income" to "Pemasukan", "transfer" to "Transfer")
            val selectedIdx = tabs.indexOfFirst { it.first == form.txType }.coerceAtLeast(0)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE5E7EB))
                    .padding(4.dp),
            ) {
                val tabWidth = maxWidth / 3
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIdx,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                    label = "tab_slide",
                )
                // Sliding indicator
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(36.dp)
                        .offset(x = indicatorOffset)
                        .clip(RoundedCornerShape(11.dp))
                        .background(CardWhite),
                )
                Row {
                    tabs.forEachIndexed { i, (value, label) ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) {
                                    form = form.copy(
                                        txType   = value,
                                        category = when (value) {
                                            "income"   -> "Gaji"
                                            "expense"  -> "Makan & Minum"
                                            else       -> form.category
                                        }
                                    )
                                },
                        ) {
                            Text(
                                label,
                                fontSize   = 12.sp, fontWeight = FontWeight.Bold,
                                color      = if (form.txType == value) TextDark else TextLight,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Transfer form ──────────────────────────────────────────────────
            if (form.txType == "transfer") {
                TransferFields(form = form, accounts = accounts, onFormChange = { form = it })
            } else {
                // ── Amount ────────────────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value         = form.amountStr.fmtDisplay(),
                            onValueChange = { raw ->
                                form = form.copy(amountStr = raw.replace("[^0-9]".toRegex(), ""))
                            },
                            placeholder   = { Text("0", fontSize = 28.sp, color = TextLight) },
                            textStyle     = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors        = clearFieldColors(),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                        )
                    }
                }

                // ── Note / merchant ───────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                FormCard(label = "CATATAN / NAMA TOKO") {
                    OutlinedTextField(
                        value         = form.note,
                        onValueChange = ::handleNoteChange,
                        placeholder   = { Text("cth: Indomaret, GoFood...", color = TextLight) },
                        colors        = clearFieldColors(),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(10.dp))
                // Detection banner
                detectionBanner?.let { cat ->
                    AutoDetectBadge(category = cat)
                }

                // ── Category grid ─────────────────────────────────────────────
                FormCard(label = "KATEGORI") {

                    val chunked = cats.chunked(4)
                    chunked.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier              = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        ) {
                            row.forEach { cat ->
                                val active = form.category == cat
                                val config = com.dompetku.ui.components.catConfig(cat)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) GreenLight else Color(0xFFF8FAFC))
                                        .clickable { form = form.copy(category = cat) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                ) {
                                    CategoryBubble(category = cat, size = 26.dp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text       = cat,
                                        fontSize   = 7.sp, fontWeight = FontWeight.SemiBold,
                                        color      = if (active) GreenPrimary else TextMedium,
                                        maxLines   = 2,
                                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                            // Fill empty cells
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // ── Account + Date ─────────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    FormCard(label = "AKUN", modifier = Modifier.weight(1f)) {
                        if (accounts.isEmpty()) {
                            Text("Buat akun dulu!", fontSize = 12.sp, color = RedExpense)
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                OutlinedTextField(
                                    value = accounts.find { it.id == form.accountId }?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    colors = clearFieldColors(),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardWhite)) {
                                    accounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text(acc.name, fontSize = 13.sp) },
                                            onClick = { form = form.copy(accountId = acc.id); expanded = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    FormCard(label = "TANGGAL", modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = form.date,
                            onValueChange = { form = form.copy(date = it) },
                            colors = clearFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── Time ──────────────────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                FormCard(label = "JAM") {
                    OutlinedTextField(
                        value = form.time,
                        onValueChange = { form = form.copy(time = it) },
                        colors = clearFieldColors(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            GreenButton(
                text    = if (isEdit) "Simpan Perubahan" else "Simpan Transaksi",
                enabled = accounts.isNotEmpty() && (form.amountStr.isNotEmpty() || form.txType == "transfer"),
                onClick = {
                    val txn = buildTransaction(form, initial, accounts)
                    if (txn != null) { onSave(txn); onDismiss() }
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Transfer fields ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferFields(form: TxnForm, accounts: List<Account>, onFormChange: (TxnForm) -> Unit) {
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        listOf("DARI AKUN" to form.fromId, "KE AKUN" to form.toId).forEachIndexed { i, (label, currentId) ->
            val acc = accounts.find { it.id == currentId }
            Column(modifier = Modifier.weight(1f)) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(CardWhite)) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(
                                        if (acc != null) Color(acc.gradientStart.toInt()) else Color(0xFFE5E7EB),
                                        if (acc != null) Color(acc.gradientEnd.toInt())   else Color(0xFFD1D5DB),
                                    ))
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 0.5.sp)
                        }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = acc?.name ?: "—",
                                onValueChange = {},
                                readOnly = true,
                                colors = clearFieldColors(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.fillMaxWidth().menuAnchor().background(Color(0xFFF8FAFC)),
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardWhite)) {
                                accounts.forEach { a ->
                                    DropdownMenuItem(
                                        text    = { Text(a.name, fontSize = 13.sp) },
                                        onClick = {
                                            if (i == 0) onFormChange(form.copy(fromId = a.id))
                                            else onFormChange(form.copy(toId = a.id))
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    FormCard(label = "JUMLAH (RP)") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value         = form.amountStr.fmtDisplay(),
                onValueChange = { raw -> onFormChange(form.copy(amountStr = raw.replace("[^0-9]".toRegex(), ""))) },
                placeholder   = { Text("0", fontSize = 28.sp, color = TextLight) },
                textStyle     = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextDark),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors        = clearFieldColors(),
                singleLine    = true,
                modifier      = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    FormCard(label = "BIAYA ADMIN (OPSIONAL)") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rp", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextLight)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value         = form.adminFee.fmtDisplay(),
                onValueChange = { raw -> onFormChange(form.copy(adminFee = raw.replace("[^0-9]".toRegex(), ""))) },
                placeholder   = { Text("0 — isi jika ada biaya transfer", color = TextLight) },
                textStyle     = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors        = clearFieldColors(),
                singleLine    = true,
                modifier      = Modifier.weight(1f),
            )
        }
    }
    // Admin fee total summary
    val amt = form.amountStr.toLongOrNull() ?: 0L
    val fee = form.adminFee.toLongOrNull() ?: 0L
    if (fee > 0) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFFBEB))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("Total keluar dari akun pengirim", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
            Text("Rp ${(amt + fee).toString().reversed().chunked(3).joinToString(".").reversed()}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        FormCard(label = "TANGGAL", modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = form.date, onValueChange = { onFormChange(form.copy(date = it)) }, colors = clearFieldColors(), singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        FormCard(label = "CATATAN (OPSIONAL)", modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = form.note, onValueChange = { onFormChange(form.copy(note = it)) }, placeholder = { Text("cth: Transfer gaji", color = TextLight) }, colors = clearFieldColors(), singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Auto detect badge ─────────────────────────────────────────────────────────
@Composable
private fun AutoDetectBadge(category: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(GreenLight)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Icon(PhosphorIcons.Regular.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(13.dp))
        Text("Auto-detect: $category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
    }
}

// ── Build Transaction from form ───────────────────────────────────────────────
private fun buildTransaction(form: TxnForm, initial: Transaction?, accounts: List<Account>): Transaction? {
    val id = initial?.id ?: java.util.UUID.randomUUID().toString()
    return when (form.txType) {
        "transfer" -> {
            val amt = form.amountStr.toLongOrNull() ?: return null
            val fee = form.adminFee.toLongOrNull() ?: 0L
            if (form.fromId.isEmpty() || form.toId.isEmpty() || form.fromId == form.toId) return null
            Transaction(
                id       = id, type = com.dompetku.domain.model.TransactionType.transfer,
                amount   = amt, adminFee = fee, category = "Transfer",
                note     = form.note, date = form.date, time = form.time,
                accountId = form.fromId, fromId = form.fromId, toId = form.toId,
            )
        }
        else -> {
            val amt = form.amountStr.toLongOrNull() ?: return null
            if (form.accountId.isEmpty()) return null
            Transaction(
                id        = id,
                type      = if (form.txType == "income") com.dompetku.domain.model.TransactionType.income else com.dompetku.domain.model.TransactionType.expense,
                amount    = amt, category = form.category,
                note      = form.note, date = form.date, time = form.time,
                accountId = form.accountId, detected = form.detected,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
private fun FormCard(
    label:    String   = "",
    modifier: Modifier = Modifier,
    content:  @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (label.isNotEmpty()) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        content()
    }
}

// ── Format helper — raw digits → "40.000" display ──────────────────────────
private fun String.fmtDisplay(): String =
    this.toLongOrNull()?.let {
        java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).format(it)
    } ?: this

@Composable
private fun clearFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Color.Transparent,
    unfocusedBorderColor    = Color.Transparent,
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)
