package com.dompetku.ui.screen.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.UserProfile
import com.dompetku.ui.components.AppHeader
import com.dompetku.ui.components.GreenButton
import com.dompetku.ui.components.SectionLabel
import com.dompetku.ui.components.SectionRow
import com.dompetku.ui.components.Toggle
import com.dompetku.ui.theme.*
import com.dompetku.util.PinHasher

private val JOBS = listOf(
    "Pelajar / Mahasiswa","Karyawan Swasta","PNS / ASN","Wirausaha",
    "Freelancer","Ibu Rumah Tangga","Profesional (Dokter/Pengacara/dll)","Lainnya",
)
private val EDUS = listOf(
    "SD / Sederajat","SMP / Sederajat","SMA / Sederajat",
    "D3","S1","S2","S3",
)

@Composable
fun ProfileScreen(
    onNavigateToMiniGame: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs
    val profile = prefs.userProfile

    var showPinSheet      by remember { mutableStateOf(false) }
    var showSetPinSheet   by remember { mutableStateOf(false) }
    var showEditSheet     by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var tapCount          by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        AppHeader(title = "Profil", showDate = false)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            // ── Avatar + name ──────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark))),
                ) {
                    Icon(PhosphorIcons.Regular.UserCircle, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    profile.name.ifEmpty { "Nama Pengguna" },
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextDark,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = "${state.txnCount} transaksi · ${state.accountCount} akun",
                    fontSize   = 12.sp,
                    color      = TextMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Profil section ─────────────────────────────────────────────────
            // PROFIL SAYA — label + Edit button di luar card (sejajar)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Text("PROFIL SAYA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 1.sp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenLight)
                        .clickable { showEditSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }
            ProfileCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileInfoChip("UMUR",
                        if (profile.age > 0) "${profile.age} tahun" else "—",
                        modifier = Modifier.weight(1f))
                    ProfileInfoChip("PEKERJAAN",
                        profile.job.ifEmpty { "—" },
                        modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                ProfileInfoChip("PENDIDIKAN",
                    profile.edu.ifEmpty { "—" },
                    modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(12.dp))

            // ── Keamanan ───────────────────────────────────────────────────────
            SectionLabel("KEAMANAN")
            ProfileCard {
                SectionRow(
                    icon = PhosphorIcons.Regular.Lock, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFF59E0B),
                    title    = "Kunci PIN",
                    subtitle = if (prefs.pinEnabled) "Aktif" else "Nonaktif",
                    rightContent = {
                        Toggle(
                            checked  = prefs.pinEnabled,
                            onToggle = {
                                if (!prefs.pinEnabled) showSetPinSheet = true
                                else { viewModel.setPinEnabled(false); viewModel.setBioEnabled(false) }
                            }
                        )
                    },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Fingerprint, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF8B5CF6),
                    title    = "Biometrik",
                    subtitle = if (!prefs.pinEnabled) "Aktifkan PIN dulu" else null,
                    rightContent = {
                        Toggle(
                            checked  = prefs.bioEnabled && prefs.pinEnabled,
                            onToggle = { if (prefs.pinEnabled) viewModel.setBioEnabled(!prefs.bioEnabled) }
                        )
                    },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Key, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF3B82F6),
                    title   = "Ganti PIN",
                    onClick = { if (prefs.pinEnabled) showPinSheet = true },
                    isLast  = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Preferensi ─────────────────────────────────────────────────────
            SectionLabel("PREFERENSI")
            ProfileCard {
                SectionRow(
                    icon = PhosphorIcons.Regular.SpeakerHigh, iconBg = Color(0xFFD1FAE5), iconTint = GreenPrimary,
                    title = "Suara Transaksi",
                    rightContent = { Toggle(checked = prefs.soundEnabled, onToggle = { viewModel.setSoundEnabled(!prefs.soundEnabled) }) },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Translate, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF3B82F6),
                    title = "Bahasa",
                    rightContent = { LanguageToggle(lang = prefs.lang, onToggle = { viewModel.setLang(it) }) },
                    isLast = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Data ───────────────────────────────────────────────────────────
            SectionLabel("DATA")
            ProfileCard {
                SectionRow(
                    icon = PhosphorIcons.Regular.Trash, iconBg = RedLight, iconTint = RedExpense,
                    title    = "Hapus Semua Data",
                    subtitle = "Transaksi & akun akan dihapus permanen",
                    isDanger = true,
                    onClick  = { showDeleteConfirm = true },
                    isLast   = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Tentang ────────────────────────────────────────────────────────
            SectionLabel("TENTANG")
            ProfileCard {
                SectionRow(
                    icon     = PhosphorIcons.Regular.Star, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFF59E0B),
                    title    = "DompetKu",
                    subtitle = "Versi 1.0 — Full Revision",
                    isLast   = true,
                    onClick  = {
                        tapCount++
                        if (tapCount >= 10) { onNavigateToMiniGame(); tapCount = 0 }
                    },
                )
            }
        }
    }

    // ── Edit profile sheet ────────────────────────────────────────────────────
    if (showEditSheet) {
        EditProfileSheet(
            profile   = profile,
            onDismiss = { showEditSheet = false },
            onSave    = { updated ->
                viewModel.saveProfile(updated)
                showEditSheet = false
            },
        )
    }

    // ── Set PIN sheet ─────────────────────────────────────────────────────────
    if (showSetPinSheet) {
        ChangePinSheet(
            title     = "Buat PIN Baru",
            onDismiss = { showSetPinSheet = false },
            onSave    = {
                viewModel.setPinHash(PinHasher.hash(it))
                viewModel.setPinEnabled(true)
                showSetPinSheet = false
            }
        )
    }

    // ── Change PIN sheet ──────────────────────────────────────────────────────
    if (showPinSheet) {
        ChangePinSheet(onDismiss = { showPinSheet = false }, onSave = { viewModel.setPinHash(PinHasher.hash(it)); showPinSheet = false })
    }

    // ── Delete confirm ────────────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title  = { Text("Hapus Semua Data?", fontWeight = FontWeight.ExtraBold) },
            text   = { Text("Semua transaksi dan akun akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAllData(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)) { Text("Hapus Semua") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } },
        )
    }
}

// ── Change PIN sheet ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePinSheet(title: String = "Ganti PIN", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newPin     by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val mismatch   = confirmPin.length == 6 && newPin != confirmPin

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PageBg,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, modifier = Modifier.padding(bottom = 16.dp))
            OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                label = { Text("PIN Baru (6 digit)") }, singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                label = { Text("Konfirmasi PIN") }, singleLine = true,
                isError = mismatch,
                supportingText = { if (mismatch) Text("PIN tidak cocok", color = RedExpense) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            GreenButton(text = "Simpan PIN", enabled = newPin.length == 6 && newPin == confirmPin, onClick = { onSave(newPin) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Edit Profile Sheet ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    profile:   UserProfile,
    onSave:    (UserProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var age  by remember { mutableStateOf(if (profile.age > 0) profile.age.toString() else "") }
    var job  by remember { mutableStateOf(profile.job) }
    var edu  by remember { mutableStateOf(profile.edu) }

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Text("Edit Profil", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5E7EB)),
                ) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(14.dp))
                }
            }

            Text("NAMA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                placeholder = { Text("Nama kamu") },
                shape  = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary, unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Text("USIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = age, onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) age = it },
                singleLine = true, placeholder = { Text("Contoh: 22") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape  = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary, unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Text("PEKERJAAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(4.dp))
            var expandJob by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expandJob, onExpandedChange = { expandJob = it }) {
                OutlinedTextField(
                    value = job.ifEmpty { "— Pilih —" }, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandJob) },
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary, unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite,
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = expandJob, onDismissRequest = { expandJob = false }, modifier = Modifier.background(CardWhite)) {
                    JOBS.forEach { opt -> DropdownMenuItem(text = { Text(opt, fontSize = 13.sp) }, onClick = { job = opt; expandJob = false }) }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("PENDIDIKAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(4.dp))
            var expandEdu by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expandEdu, onExpandedChange = { expandEdu = it }) {
                OutlinedTextField(
                    value = edu.ifEmpty { "— Pilih —" }, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandEdu) },
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary, unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite,
                    ),
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = expandEdu, onDismissRequest = { expandEdu = false }, modifier = Modifier.background(CardWhite)) {
                    EDUS.forEach { opt -> DropdownMenuItem(text = { Text(opt, fontSize = 13.sp) }, onClick = { edu = opt; expandEdu = false }) }
                }
            }

            Spacer(Modifier.height(20.dp))

            GreenButton(
                text    = "Simpan Profil",
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    onSave(UserProfile(name = name.trim(), age = age.toIntOrNull() ?: 0, job = job, edu = edu))
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Language toggle with slide animation ──────────────────────────────────────
@Composable
private fun LanguageToggle(lang: String, onToggle: (String) -> Unit) {
    val isEn = lang == "en"
    val thumbOffset by animateDpAsState(
        targetValue   = if (isEn) 36.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label         = "lang_slide",
    )
    Box(
        modifier = Modifier
            .size(width = 74.dp, height = 30.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onToggle(if (isEn) "id" else "en") },
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .padding(vertical = 2.dp)
                .size(width = 36.dp, height = 26.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(CardWhite)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("ID", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                color = if (!isEn) GreenPrimary else TextLight)
            Text("EN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                color = if (isEn) GreenPrimary else TextLight)
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite),
        content = content,
    )
}

@Composable
private fun ProfileInfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}
