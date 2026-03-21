package com.dompetku.ui.screen.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dompetku.BuildConfig
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.util.AccountResolution
import com.dompetku.util.DetectedAccount
import com.dompetku.util.ImportResult
import com.dompetku.util.SmartImportResult
import kotlinx.coroutines.flow.collectLatest
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.R
import com.dompetku.domain.model.UserProfile
import com.dompetku.ui.components.AppHeader
import com.dompetku.ui.components.GreenButton
import com.dompetku.ui.components.SectionLabel
import com.dompetku.ui.components.SectionRow
import com.dompetku.ui.components.Toggle
import com.dompetku.ui.theme.*
import com.dompetku.util.HapticHelper
import com.dompetku.util.PinHasher

private val JOBS = listOf(
    "Pelajar / Mahasiswa", "Guru / Dosen", "PNS / ASN", "Karyawan Swasta",
    "Wiraswasta / Pengusaha", "Freelancer", "Dokter / Tenaga Medis",
    "Engineer / Programmer", "Desainer / Kreator", "Buruh / Pekerja Harian",
    "Ibu Rumah Tangga", "Pensiunan", "Belum Bekerja", "Lainnya",
)
private val EDUS = listOf(
    "SD / Sederajat", "SMP / Sederajat", "SMA / SMK / Sederajat",
    "Diploma (D1-D3)", "Sarjana (S1)", "Magister (S2)", "Doktor (S3)",
    "Tidak Ingin Menyebutkan",
)

@Composable
fun ProfileScreen(
    onNavigateToMiniGame: () -> Unit,
    onNavigateToPinSetup: (Boolean) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs
    val profile = prefs.userProfile

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var showPinSheet       by remember { mutableStateOf(false) }
    var showSetPinSheet    by remember { mutableStateOf(false) }
    var showEditSheet      by remember { mutableStateOf(false) }
    var showDeleteConfirm  by remember { mutableStateOf(false) }
    var tapCount           by remember { mutableIntStateOf(0) }

    // Export/Import state
    var isExporting          by remember { mutableStateOf(false) }
    var isImporting          by remember { mutableStateOf(false) }
    var showExportInfoDialog by remember { mutableStateOf(false) }
    var showImportInfoDialog by remember { mutableStateOf(false) }
    var importPreview        by remember { mutableStateOf<ImportResult?>(null) }
    var smartImportPreview   by remember { mutableStateOf<SmartImportResult?>(null) }
    var snackbarMessage      by remember { mutableStateOf<String?>(null) }
    val snackbarHostState    = remember { SnackbarHostState() }

    // POST_NOTIFICATIONS permission launcher (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Enable notification regardless — if denied, notifications just won't show
        // but the toggle state is saved so user can try again
        viewModel.setNotifEnabled(granted)
    }

    // Import file picker — uses smart import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isImporting = true
            viewModel.smartPreviewImport(it)
        }
    }

    // Collect one-shot events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.eiEvent.collectLatest { event ->
            isExporting = false
            isImporting = false
            when (event) {
                is com.dompetku.ui.screen.profile.EiEvent.ExportSuccess -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Bagikan file ekspor"))
                }
                is com.dompetku.ui.screen.profile.EiEvent.ImportPreviewReady -> {
                    importPreview = event.result
                }
                is com.dompetku.ui.screen.profile.EiEvent.SmartImportPreviewReady -> {
                    isImporting = false
                    smartImportPreview = event.result
                }
                is com.dompetku.ui.screen.profile.EiEvent.ImportCommitted -> {
                    snackbarMessage = "Import selesai: ${event.txnCount} transaksi, ${event.accCount} akun" +
                        if (event.errCount > 0) " (${event.errCount} baris dilewati)" else ""
                }
                is com.dompetku.ui.screen.profile.EiEvent.Failure -> {
                    snackbarMessage = event.message
                }
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    fun promptBiometricEnable() {
        val act = activity ?: return
        val executor = ContextCompat.getMainExecutor(act)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.setBioEnabled(true)
            }
        }
        val allowedAuth = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.pin_biometric_enable_title))
            .setSubtitle(context.getString(R.string.pin_biometric_enable_subtitle))
            .setNegativeButtonText(context.getString(R.string.cancel_label))
            .setAllowedAuthenticators(allowedAuth)
            .build()
        BiometricPrompt(act, executor, callback).authenticate(promptInfo)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        AppHeader(title = stringResource(R.string.profile_title), showDate = false)

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
                Text(stringResource(R.string.profile_my_profile), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 1.sp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenLight)
                        .clickable { showEditSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(stringResource(R.string.edit_label), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }
            ProfileCard(innerPadding = true) {
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
            SectionLabel(stringResource(R.string.profile_security))
            ProfileCard {
                SectionRow(
                    icon = PhosphorIcons.Regular.Lock, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFF59E0B),
                    title    = stringResource(R.string.profile_pin_lock),
                    subtitle = if (prefs.pinEnabled) stringResource(R.string.profile_enabled) else stringResource(R.string.profile_disabled),
                    rightContent = {
                        Toggle(
                            checked  = prefs.pinEnabled,
                            onToggle = {
                                if (!prefs.pinEnabled) {
                                    HapticHelper.toggleOn(context)
                                    onNavigateToPinSetup(false)
                                } else {
                                    HapticHelper.toggleOff(context)
                                    viewModel.setPinEnabled(false)
                                    viewModel.setBioEnabled(false)
                                }
                            }
                        )
                    },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Fingerprint, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF8B5CF6),
                    title    = stringResource(R.string.profile_biometric),
                    subtitle = if (!prefs.pinEnabled) stringResource(R.string.profile_enable_pin_first) else null,
                    rightContent = {
                        Toggle(
                            checked  = prefs.bioEnabled && prefs.pinEnabled,
                            onToggle = {
                                if (!prefs.pinEnabled) return@Toggle
                                if (prefs.bioEnabled) {
                                    HapticHelper.toggleOff(context)
                                    viewModel.setBioEnabled(false)
                                } else {
                                    HapticHelper.toggleOn(context)
                                    promptBiometricEnable()
                                }
                            }
                        )
                    },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Key, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF3B82F6),
                    title   = stringResource(R.string.profile_change_pin),
                    onClick = { if (prefs.pinEnabled) onNavigateToPinSetup(true) },
                    isLast  = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Preferensi ─────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.profile_preferences))
            ProfileCard {
                SectionRow(
                    icon = PhosphorIcons.Regular.SpeakerHigh, iconBg = Color(0xFFD1FAE5), iconTint = GreenPrimary,
                    title = stringResource(R.string.profile_sound),
                    rightContent = { Toggle(checked = prefs.soundEnabled, onToggle = { viewModel.setSoundEnabled(!prefs.soundEnabled) }) },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Bell, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFF59E0B),
                    title = stringResource(R.string.profile_daily_reminder),
                    subtitle = stringResource(R.string.profile_daily_reminder_desc),
                    rightContent = {
                        Toggle(
                            checked  = prefs.notifEnabled,
                            onToggle = {
                                if (!prefs.notifEnabled) {
                                    // Android 13+: request POST_NOTIFICATIONS before enabling
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotifEnabled(true)
                                    }
                                } else {
                                    viewModel.setNotifEnabled(false)
                                }
                            }
                        )
                    },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon = PhosphorIcons.Regular.Translate, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF3B82F6),
                    title = stringResource(R.string.profile_language),
                    rightContent = { LanguageToggle(lang = prefs.lang, onToggle = { viewModel.setLang(it) }) },
                    isLast = true,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Data ───────────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.profile_data))
            ProfileCard {
                SectionRow(
                    icon     = PhosphorIcons.Regular.ArrowSquareOut,
                    iconBg   = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF3B82F6),
                    title    = stringResource(R.string.profile_export),
                    subtitle = if (isExporting) "Sedang mengekspor..." else "Simpan ke XLSX",
                    onClick  = { if (!isExporting) showExportInfoDialog = true },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
                SectionRow(
                    icon     = PhosphorIcons.Regular.ArrowSquareIn,
                    iconBg   = Color(0xFFD1FAE5),
                    iconTint = GreenPrimary,
                    title    = stringResource(R.string.profile_import),
                    subtitle = if (isImporting) "Membaca file..." else "Muat dari Money Manager XLSX",
                    onClick  = { if (!isImporting) showImportInfoDialog = true },
                )
                HorizontalDivider(color = Color(0xFFF8FAFC))
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
                    subtitle = "Versi 0.0703.5G",
                    isLast   = false,
                    onClick  = {
                        tapCount++
                        if (tapCount >= 10) { onNavigateToMiniGame(); tapCount = 0 }
                    },
                )
                if (BuildConfig.DEBUG) {
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    SectionRow(
                        icon     = PhosphorIcons.Regular.BellRinging,
                        iconBg   = Color(0xFFFEE2E2),
                        iconTint = Color(0xFFEF4444),
                        title    = "[DEBUG] Test Notifikasi",
                        subtitle = "Kirim semua 5 slot sekarang",
                        isLast   = true,
                        onClick  = { viewModel.testNotifications() },
                    )
                }
            }
        }
    }

    // Snackbar host
    SnackbarHost(
        hostState = snackbarHostState,
        modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
    )

    // ── Loading overlay (import / export sedang berjalan) ──────────────────
    if (isImporting || isExporting) {
        DompetKuLoadingOverlay(
            message = if (isImporting) "Membaca data..." else "Mengekspor data..."
        )
    }

    } // end Box

    // ── Smart Import dialog ───────────────────────────────────────────────
    smartImportPreview?.let { result ->
        SmartImportDialog(
            result           = result,
            existingAccounts = state.accounts,
            onDismiss        = { smartImportPreview = null },
            onCommit         = { resolutions, replace ->
                viewModel.commitSmartImport(result, resolutions, replace)
                smartImportPreview = null
            },
        )
    }

    // ── Import preview dialog (legacy) ─────────────────────────────────
    importPreview?.let { preview ->
        ImportPreviewDialog(
            preview   = preview,
            onDismiss = { importPreview = null },
            onMerge   = {
                viewModel.commitImport(preview, replace = false)
                importPreview = null
            },
            onReplace = {
                viewModel.commitImport(preview, replace = true)
                importPreview = null
            },
        )
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

    // ── Export info dialog ─────────────────────────────────────────────────────
    if (showExportInfoDialog) {
        ExportInfoDialog(
            txnCount  = state.txnCount,
            accCount  = state.accountCount,
            onDismiss = { showExportInfoDialog = false },
            onExport  = {
                showExportInfoDialog = false
                isExporting = true
                viewModel.triggerExport()
            },
        )
    }

    // ── Import info dialog ─────────────────────────────────────────────────────
    if (showImportInfoDialog) {
        ImportInfoDialog(
            onDismiss    = { showImportInfoDialog = false },
            onPickFile   = {
                showImportInfoDialog = false
                importLauncher.launch(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            },
        )
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

// ── Import preview dialog ───────────────────────────────────────────────────
@Composable
private fun ImportPreviewDialog(
    preview:   ImportResult,
    onDismiss: () -> Unit,
    onMerge:   () -> Unit,
    onReplace: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = PageBg,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Text("Konfirmasi Import", fontWeight = FontWeight.ExtraBold, color = TextDark)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Summary counts
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDBEAFE))
                            .padding(10.dp),
                    ) {
                        Column {
                            Text("${preview.transactions.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B82F6))
                            Text("transaksi", fontSize = 11.sp, color = TextMedium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenLight)
                            .padding(10.dp),
                    ) {
                        Column {
                            Text("${preview.accounts.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = GreenPrimary)
                            Text("akun", fontSize = 11.sp, color = TextMedium)
                        }
                    }
                }
                if (preview.errors.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedLight)
                            .padding(10.dp),
                    ) {
                        Text(
                            text = "⚠️ ${preview.errors.size} baris tidak bisa dibaca dan akan dilewati.",
                            fontSize = 12.sp, color = RedExpense,
                        )
                    }
                }
                Text(
                    text = "Pilih mode import:",
                    fontSize = 13.sp, color = TextMedium,
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Button(
                    onClick  = onMerge,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) {
                    Text("Gabungkan", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick  = onReplace,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedExpense),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, RedExpense),
                ) {
                    Text("Ganti Semua Data", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Batal", color = TextMedium)
                }
            }
        },
    )
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
            var expandJob   by remember { mutableStateOf(false) }
            var customJob   by remember { mutableStateOf(if (job !in JOBS && job.isNotEmpty()) job else "") }
            val isCustomJob  = job == "Lainnya" || (job.isNotEmpty() && job !in JOBS)
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
            // Free-text input when "Lainnya" selected
            if (isCustomJob) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value         = customJob,
                    onValueChange = { customJob = it; job = it.ifEmpty { "Lainnya" } },
                    placeholder   = { Text("Tulis pekerjaanmu...", color = TextLight) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenPrimary, unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
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
private fun ProfileInfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = TextLight, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileCard(
    innerPadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .then(if (innerPadding) Modifier.padding(14.dp) else Modifier),
        content = content,
    )
}

// ── Smart Import Dialog ─────────────────────────────────────────────────────────────────

@Composable
private fun SmartImportDialog(
    result:           SmartImportResult,
    existingAccounts: List<Account>,
    onDismiss:        () -> Unit,
    onCommit:         (resolutions: Map<String, AccountResolution>, replace: Boolean) -> Unit,
) {
    // Auto-resolve high-confidence matches (≥ 0.7) upfront
    val initialResolutions = remember(result) {
        result.detectedAccounts.associate { da ->
            da.rawName to (
                if (da.suggestedMatch != null && da.matchScore >= 0.7f)
                    AccountResolution.UseExisting(da.suggestedMatch)
                else
                    AccountResolution.Skip  // placeholder — user resolves
            )
        }
    }

    var resolutions  by remember { mutableStateOf(initialResolutions) }
    val needsResolve  = result.detectedAccounts.filter { it.matchScore < 0.7f }
    var step         by remember { mutableIntStateOf(if (needsResolve.isNotEmpty()) 0 else 1) }
    var expandCatMap by remember { mutableStateOf(false) }

    // step 0: account resolution
    if (step == 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor   = PageBg,
            shape            = RoundedCornerShape(24.dp),
            title = { Text("Cocokkan Akun", fontWeight = FontWeight.ExtraBold, color = TextDark) },
            text  = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Beberapa akun di file tidak dikenali. Tentukan ke mana transaksi mereka akan disimpan.",
                        fontSize = 13.sp, color = TextMedium,
                    )
                    needsResolve.forEach { da ->
                        AccountResolutionCard(
                            detected         = da,
                            existingAccounts = existingAccounts,
                            current          = resolutions[da.rawName],
                            onChange         = { resolution ->
                                resolutions = resolutions + (da.rawName to resolution)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick  = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled  = needsResolve.all { resolutions[it.rawName] !is AccountResolution.Skip ||
                            resolutions[it.rawName] is AccountResolution.Skip },
                    ) { Text("Lanjut → Ringkasan", fontWeight = FontWeight.Bold) }
                    TextButton(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Batal", color = TextMedium) }
                }
            },
        )
        return
    }

    // step 1: summary + confirm
    val totalTxn  = result.transactions.size
    val willSkip  = result.detectedAccounts.count { resolutions[it.rawName] is AccountResolution.Skip }
    val willSave  = totalTxn - result.transactions.count { txn ->
        resolutions[txn.rawAccountName] is AccountResolution.Skip
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = PageBg,
        shape            = RoundedCornerShape(24.dp),
        title = { Text("Konfirmasi Import", fontWeight = FontWeight.ExtraBold, color = TextDark) },
        text  = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // File info chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(10.dp),
                ) {
                    Text(result.fileInfo, fontSize = 11.sp, color = TextMedium)
                }

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartStatChip(
                        value = "$willSave",
                        label = "transaksi",
                        bg    = Color(0xFFDBEAFE),
                        fg    = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                    )
                    SmartStatChip(
                        value = "${result.detectedAccounts.size}",
                        label = "akun",
                        bg    = GreenLight,
                        fg    = GreenPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    SmartStatChip(
                        value = "${result.categoryMappings.size}",
                        label = "kategori",
                        bg    = Color(0xFFEDE9FE),
                        fg    = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Auto-sign info
                if (result.autoSignCount > 0) {
                    SmartInfoBanner(
                        text = "ℹ️ ${result.autoSignCount} transaksi bertanda negatif otomatis dijadikan Pengeluaran.",
                        bg   = Color(0xFFFEF3C7),
                        fg   = Color(0xFFB45309),
                    )
                }

                // Extra fields info
                if (result.extraFieldCount > 0) {
                    SmartInfoBanner(
                        text = "📋 ${result.extraFieldCount} transaksi punya kolom tambahan — dipindah ke Catatan.",
                        bg   = Color(0xFFDBEAFE),
                        fg   = Color(0xFF3B82F6),
                    )
                }

                // Skipped accounts warning
                if (willSkip > 0) {
                    SmartInfoBanner(
                        text = "⚠️ $willSkip akun dilewati — transaksi mereka tidak akan diimpor.",
                        bg   = RedLight,
                        fg   = RedExpense,
                    )
                }

                // Errors
                if (result.errors.isNotEmpty()) {
                    SmartInfoBanner(
                        text = "⚠️ ${result.errors.size} baris tidak bisa dibaca dan akan dilewati.",
                        bg   = RedLight,
                        fg   = RedExpense,
                    )
                }

                // Category mapping (collapsible)
                if (result.categoryMappings.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { expandCatMap = !expandCatMap }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Pemetaan Kategori", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMedium)
                        Icon(
                            if (expandCatMap) PhosphorIcons.Regular.CaretUp else PhosphorIcons.Regular.CaretDown,
                            null, tint = TextLight, modifier = Modifier.size(14.dp),
                        )
                    }
                    if (expandCatMap) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            result.categoryMappings.forEach { (raw, mapped) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        raw.ifBlank { "(kosong)" },
                                        fontSize = 11.sp, color = TextMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "→ $mapped",
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = if (mapped == "Lainnya") TextLight else GreenPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (needsResolve.isNotEmpty()) {
                    TextButton(
                        onClick  = { step = 0 },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("← Kembali ke Cocokkan Akun", color = Color(0xFF3B82F6)) }
                }
                Button(
                    onClick  = { onCommit(resolutions, false) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) { Text("Gabungkan", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick  = { onCommit(resolutions, true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedExpense),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, RedExpense),
                ) { Text("Ganti Semua Data", fontWeight = FontWeight.Bold) }
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Batal", color = TextMedium) }
            }
        },
    )
}

@Composable
private fun AccountResolutionCard(
    detected:         DetectedAccount,
    existingAccounts: List<Account>,
    current:          AccountResolution?,
    onChange:         (AccountResolution) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header: rawName + txn count
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    detected.rawName,
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextDark,
                )
                Text(
                    "${detected.transactionCount} transaksi",
                    fontSize = 11.sp, color = TextMedium,
                )
            }
            if (detected.suggestedMatch != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${(detected.matchScore * 100).toInt()}% cocok",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary,
                    )
                }
            }
        }

        // Suggested match (if any)
        if (detected.suggestedMatch != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (current is AccountResolution.UseExisting &&
                            current.account.id == detected.suggestedMatch.id)
                            GreenLight else Color(0xFFF1F5F9)
                    )
                    .clickable { onChange(AccountResolution.UseExisting(detected.suggestedMatch)) }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pakai \"${detected.suggestedMatch.name}\"",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark,
                )
                if (current is AccountResolution.UseExisting &&
                    current.account.id == detected.suggestedMatch.id) {
                    Icon(PhosphorIcons.Regular.Check, null, tint = GreenPrimary,
                        modifier = Modifier.size(16.dp))
                }
            }
        }

        // Pick another account
        if (existingAccounts.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (current is AccountResolution.UseExisting &&
                            current.account.id != detected.suggestedMatch?.id)
                            Color(0xFFDBEAFE) else Color(0xFFF1F5F9)
                    )
                    .clickable { expanded = true }
                    .padding(10.dp),
            ) {
                val chosenLabel = if (current is AccountResolution.UseExisting &&
                    current.account.id != detected.suggestedMatch?.id)
                    "Pakai \"${current.account.name}\""
                else "Pilih akun lain..."
                Text(chosenLabel, fontSize = 12.sp, color = TextMedium)
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    existingAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text    = { Text(acc.name, fontSize = 13.sp) },
                            onClick = {
                                onChange(AccountResolution.UseExisting(acc))
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        // Create new / Skip row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (current is AccountResolution.CreateNew)
                            Color(0xFFEDE9FE) else Color(0xFFF1F5F9)
                    )
                    .clickable { onChange(AccountResolution.CreateNew(detected.rawName)) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(PhosphorIcons.Regular.Plus, null,
                        tint = if (current is AccountResolution.CreateNew) Color(0xFF8B5CF6) else TextLight,
                        modifier = Modifier.size(13.dp))
                    Text(
                        "Buat akun baru",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (current is AccountResolution.CreateNew) Color(0xFF8B5CF6) else TextLight,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (current is AccountResolution.Skip)
                            RedLight else Color(0xFFF1F5F9)
                    )
                    .clickable { onChange(AccountResolution.Skip) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(PhosphorIcons.Regular.X, null,
                        tint = if (current is AccountResolution.Skip) RedExpense else TextLight,
                        modifier = Modifier.size(13.dp))
                    Text(
                        "Lewati",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (current is AccountResolution.Skip) RedExpense else TextLight,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartStatChip(
    value: String, label: String, bg: Color, fg: Color, modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(10.dp),
    ) {
        Column {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = fg)
            Text(label, fontSize = 11.sp, color = TextMedium)
        }
    }
}

@Composable
private fun SmartInfoBanner(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(10.dp),
    ) {
        Text(text, fontSize = 12.sp, color = fg)
    }
}

// ── DompetKu Loading Overlay ───────────────────────────────────────────────────────────
@Composable
private fun DompetKuLoadingOverlay(message: String) {
    // Animated pulsing scale for the logo
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = InfiniteRepeatableSpec(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading_scale",
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = InfiniteRepeatableSpec(
            animation  = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loading_rotation",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.92f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Logo + spinning arc
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize().graphicsLayer { this.rotationZ = rotation }
                ) {
                    drawArc(
                        color      = GreenPrimary,
                        startAngle = 0f,
                        sweepAngle = 260f,
                        useCenter  = false,
                        style      = Stroke(
                            width = 4.dp.toPx(),
                            cap   = androidx.compose.ui.graphics.StrokeCap.Round,
                        ),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer { this.scaleX = scale; this.scaleY = scale }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark))),
                ) {
                    com.dompetku.ui.components.DompetKuLogo(
                        size  = 36.dp,
                        color = Color.White,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text       = message,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TextDark,
                )
                Text(
                    text     = "Mohon tunggu sebentar",
                    fontSize = 12.sp,
                    color    = TextMedium,
                )
            }
        }
    }
}

// ── Export Info Dialog ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportInfoDialog(
    txnCount:  Int,
    accCount:  Int,
    onDismiss: () -> Unit,
    onExport:  () -> Unit,
) {
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
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFDBEAFE)),
                ) {
                    Icon(PhosphorIcons.Regular.ArrowSquareOut, null,
                        tint = Color(0xFF3B82F6), modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Ekspor Data", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    Text("Format XLSX · 2 sheet", fontSize = 12.sp, color = TextMedium)
                }
            }

            // Data summary
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDBEAFE)).padding(12.dp),
                ) {
                    Column {
                        Text("$txnCount", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B82F6))
                        Text("transaksi", fontSize = 11.sp, color = TextMedium)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(GreenLight).padding(12.dp),
                ) {
                    Column {
                        Text("$accCount", fontSize = 22.sp, fontWeight = FontWeight.Black, color = GreenPrimary)
                        Text("akun", fontSize = 11.sp, color = TextMedium)
                    }
                }
            }

            // Info rows
            InfoRow(icon = PhosphorIcons.Regular.Table, text = "Sheet \"Transaksi\": Tanggal, Waktu, Jenis, Nominal, Nama, Kategori, Akun, Transfer, Biaya Admin")
            InfoRow(icon = PhosphorIcons.Regular.CreditCard, text = "Sheet \"Akun\": Nama, Tipe, Saldo, Nomor Akhir, Brand")
            InfoRow(icon = PhosphorIcons.Regular.ShareNetwork, text = "File langsung dibagikan via sistem berbagi HP (WhatsApp, email, Drive, dll.)")

            // Disclaimer
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(PhosphorIcons.Regular.Warning, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Text(
                        "File ekspor menggunakan format DompetKu. Untuk impor kembali ke DompetKu, " +
                        "gunakan file ini langsung. Format ini berbeda dengan format Money Manager.",
                        fontSize = 12.sp, color = Color(0xFF92400E), lineHeight = 18.sp,
                    )
                }
            }

            // Buttons
            Button(
                onClick  = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            ) {
                Icon(PhosphorIcons.Regular.ArrowSquareOut, null,
                    modifier = Modifier.size(16.dp).padding(end = 0.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ekspor Sekarang", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Batal", color = TextMedium)
            }
        }
    }
}

// ── Import Info Dialog ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportInfoDialog(
    onDismiss:  () -> Unit,
    onPickFile: () -> Unit,
) {
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
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GreenLight),
                ) {
                    Icon(PhosphorIcons.Regular.ArrowSquareIn, null,
                        tint = GreenPrimary, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Impor Data", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    Text("Dari Money Manager · Format XLSX", fontSize = 12.sp, color = TextMedium)
                }
            }

            // Format yang didukung
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenLight).padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FORMAT YANG DIDUKUNG", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = GreenPrimary, letterSpacing = 0.5.sp)
                    Text("✓  Money Manager (Export → Excel/CSV)",
                        fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
            }

            // Info rows
            InfoRow(icon = PhosphorIcons.Regular.Calendar, text = "Kolom Date, Account, Category, Note, IDR/Amount, Income/Expense otomatis dikenali")
            InfoRow(icon = PhosphorIcons.Regular.ArrowsLeftRight, text = "Transfer-Out akan dikenali sebagai Transfer antar akun")
            InfoRow(icon = PhosphorIcons.Regular.Tag, text = "Kategori Money Manager otomatis dipetakan ke kategori DompetKu")
            InfoRow(icon = PhosphorIcons.Regular.UsersThree, text = "Akun baru yang belum ada bisa dibuat otomatis atau dihubungkan ke akun yang ada")

            // Disclaimer
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(PhosphorIcons.Regular.Warning, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Text(
                        "Pembacaan data otomatis — penempatan kategori dan jenis transaksi " +
                        "mungkin tidak 100% akurat. Selalu cek hasilnya di layar konfirmasi " +
                        "sebelum menekan Gabungkan atau Ganti Semua Data.",
                        fontSize = 12.sp, color = Color(0xFF92400E), lineHeight = 18.sp,
                    )
                }
            }

            // Buttons
            Button(
                onClick  = onPickFile,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            ) {
                Icon(PhosphorIcons.Regular.FolderOpen, null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pilih File XLSX", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Batal", color = TextMedium)
            }
        }
    }
}

// ── Info row helper ────────────────────────────────────────────────────────────────
@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)),
        ) {
            Icon(icon, null, tint = TextMedium, modifier = Modifier.size(14.dp))
        }
        Text(text, fontSize = 12.sp, color = TextDark, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}
