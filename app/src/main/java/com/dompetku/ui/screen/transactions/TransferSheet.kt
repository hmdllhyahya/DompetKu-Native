package com.dompetku.ui.screen.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.ui.components.GreenButton
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter
import com.dompetku.util.DateUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    accounts:  List<Account>,
    onDismiss: () -> Unit,
    onSave:    (Transaction) -> Unit,
) {
    var fromId      by remember { mutableStateOf(accounts.getOrNull(0)?.id ?: "") }
    var toId        by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amountStr   by remember { mutableStateOf("") }
    var adminFee    by remember { mutableStateOf("") }
    var note        by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
        attachments = (attachments + uris.map { it.toString() }).distinct().take(5)
    }

    val fromAcc = accounts.find { it.id == fromId }
    val toAcc   = accounts.find { it.id == toId }
    val amt     = amountStr.toLongOrNull() ?: 0L
    val fee     = adminFee.toLongOrNull()  ?: 0L
    val total   = amt + fee

    val fromBalance = fromAcc?.balance ?: 0L
    val insufficient = total > fromBalance

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
            // Header
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                Text("Transfer Antar Akun", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5E7EB))) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(14.dp))
                }
            }

            // From / To cards
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                AccountPickerCard(
                    label    = "DARI AKUN",
                    account  = fromAcc,
                    accounts = accounts.filter { it.id != toId },
                    onPick   = { fromId = it.id },
                    modifier = Modifier.weight(1f),
                )
                // Swap button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.align(Alignment.CenterVertically).size(36.dp).clip(RoundedCornerShape(10.dp)).background(GreenLight).clickable {
                        val tmp = fromId; fromId = toId; toId = tmp
                    },
                ) {
                    Icon(PhosphorIcons.Regular.ArrowsLeftRight, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                }
                AccountPickerCard(
                    label    = "KE AKUN",
                    account  = toAcc,
                    accounts = accounts.filter { it.id != fromId },
                    onPick   = { toId = it.id },
                    modifier = Modifier.weight(1f),
                )
            }

            // Amount
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardWhite).padding(14.dp).padding(bottom = 12.dp)) {
                Text("JUMLAH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rp", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = amountStr.fmtDisplay(),
                        onValueChange = { amountStr = it.replace("[^0-9]".toRegex(), "") },
                        placeholder   = { Text("0", fontSize = 28.sp, color = TextLight) },
                        textStyle     = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextDark),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors        = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Admin fee
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardWhite).padding(14.dp).padding(bottom = 10.dp)) {
                Text("BIAYA ADMIN (OPSIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = adminFee.fmtDisplay(),
                        onValueChange = { adminFee = it.replace("[^0-9]".toRegex(), "") },
                        placeholder   = { Text("0", color = TextLight) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors        = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Total summary row
            if (amt > 0) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (insufficient) RedLight else GreenLight).padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text("Total keluar dari ${fromAcc?.name ?: "pengirim"}", fontSize = 10.sp, color = if (insufficient) RedExpense else GreenPrimary, fontWeight = FontWeight.SemiBold)
                        if (insufficient) Text("Saldo tidak cukup!", fontSize = 10.sp, color = RedExpense, fontWeight = FontWeight.Bold)
                    }
                    Text(CurrencyFormatter.format(total), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (insufficient) RedExpense else GreenPrimary)
                }
                Spacer(Modifier.height(8.dp))
            }

            // Attachment section
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardWhite)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.fillMaxWidth().padding(bottom = if (attachments.isEmpty()) 0.dp else 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(PhosphorIcons.Regular.Paperclip, null, tint = TextLight, modifier = Modifier.size(15.dp))
                        Text("LAMPIRAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
                        if (attachments.isNotEmpty()) Text("(${attachments.size}/5)", fontSize = 9.sp, color = TextLight)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GreenLight)
                            .clickable { attachmentLauncher.launch("image/*") }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(PhosphorIcons.Regular.Plus, null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
                            Text("Tambah Foto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                }
                if (attachments.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(attachments) { uriStr ->
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = Uri.parse(uriStr), contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(99.dp)).background(Color(0xFFEF4444))
                                        .clickable { attachments = attachments.filter { it != uriStr } },
                                ) {
                                    Icon(PhosphorIcons.Regular.X, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Note
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardWhite).padding(14.dp).padding(bottom = 10.dp)) {
                Text("CATATAN (OPSIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    placeholder = { Text("cth: Transfer gaji", color = TextLight) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))

            GreenButton(
                text    = "Konfirmasi Transfer",
                enabled = amt > 0 && fromId.isNotEmpty() && toId.isNotEmpty() && fromId != toId && !insufficient,
                onClick = {
                    onSave(Transaction(
                        id            = UUID.randomUUID().toString(),
                        type          = TransactionType.transfer,
                        amount        = amt, adminFee = fee,
                        category      = "Transfer",
                        note          = note,
                        date          = DateUtils.todayStr(),
                        time          = DateUtils.nowTimeStr(),
                        accountId     = fromId, fromId = fromId, toId = toId,
                        attachmentIds = attachments,
                    ))
                    onDismiss()
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Format helper ───────────────────────────────────────────────────────────
private fun String.fmtDisplay(): String =
    this.toLongOrNull()?.let {
        java.text.NumberFormat.getNumberInstance(java.util.Locale("in", "ID")).format(it)
    } ?: this

// ── Account picker card ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerCard(
    label:    String,
    account:  Account?,
    accounts: List<Account>,
    onPick:   (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(CardWhite)) {
        // Gradient header strip
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(
                    if (account != null) Brush.linearGradient(listOf(Color(account.gradientStart.toInt()), Color(account.gradientEnd.toInt())))
                    else Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)))
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.9f), letterSpacing = 0.5.sp)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = account?.name ?: "— Pilih —",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = GreenPrimary.copy(alpha = 0.3f), unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardWhite)) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(CurrencyFormatter.compact(acc.balance), fontSize = 11.sp, color = TextLight)
                            }
                        },
                        onClick = { onPick(acc); expanded = false },
                    )
                }
            }
        }
        if (account != null) {
            Text(
                CurrencyFormatter.format(account.balance),
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (account.balance < 0) RedExpense else GreenPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
