package com.dompetku.ui.screen.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.components.DompetKuLogo
import com.dompetku.ui.theme.*

// ── Static data (mirroring JSX exactly) ──────────────────────────────────────

private data class Slide(val icon: @Composable () -> Unit, val title: String, val desc: String)

private val SLIDES = listOf(
    Slide(
        icon  = { Icon(PhosphorIcons.Regular.CurrencyCircleDollar, null, tint = Color.White, modifier = Modifier.size(56.dp)) },
        title = "Catat Keuanganmu",
        desc  = "Lacak setiap pemasukan dan pengeluaran dengan mudah dan cepat.",
    ),
    Slide(
        icon  = { Icon(PhosphorIcons.Regular.ChartBar, null, tint = Color.White, modifier = Modifier.size(56.dp)) },
        title = "Analisis Pengeluaran",
        desc  = "Visualisasi lengkap kategori pengeluaranmu setiap bulan.",
    ),
    Slide(
        icon  = { Icon(PhosphorIcons.Regular.Target, null, tint = Color.White, modifier = Modifier.size(56.dp)) },
        title = "Capai Target Tabungan",
        desc  = "Set budget bulanan dan pantau progress tabunganmu.",
    ),
)

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

private const val TOTAL_PROFILE_STEPS = 4  // name, age, job, edu
private const val SWIPE_THRESHOLD_PX  = 120f

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onDone:    () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var step         by remember { mutableIntStateOf(0) }
    var name         by remember { mutableStateOf("") }
    var age          by remember { mutableStateOf("") }
    var job          by remember { mutableStateOf("") }
    var edu          by remember { mutableStateOf("") }
    var dragStart    by remember { mutableFloatStateOf(0f) }

    val totalSteps   = SLIDES.size + TOTAL_PROFILE_STEPS
    val inSlides     = step < SLIDES.size
    val profileStep  = step - SLIDES.size   // 0..3

    val canNext = when {
        inSlides        -> true
        profileStep == 0 -> name.trim().isNotEmpty()
        profileStep == 1 -> age.isNotEmpty()
        profileStep == 2 -> job.isNotEmpty()
        profileStep == 3 -> edu.isNotEmpty()
        else             -> true
    }

    fun handleNext() {
        if (!canNext) return
        if (step < totalSteps - 1) { step++; return }
        // Last step → persist + navigate
        viewModel.completeOnboarding(name.trim(), age.toIntOrNull() ?: 0, job, edu)
        onDone()
    }

    val gradient = Brush.linearGradient(
        colors = listOf(GreenPrimary, GreenDark),
        start  = androidx.compose.ui.geometry.Offset(0f, 0f),
        end    = androidx.compose.ui.geometry.Offset(400f, 1200f),
    )

    // Back: kembali ke slide/step sebelumnya, tidak keluar app
    BackHandler(enabled = step > 0) { step-- }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (inSlides) {
            SlideContent(
                slide       = SLIDES[step],
                currentIdx  = step,
                total       = SLIDES.size,
                onNext      = { handleNext() },
                onSwipeLeft = { if (step < SLIDES.size - 1) step++ },
                onSwipeRight = { if (step > 0) step-- },
            )
        } else {
            ProfileFormContent(
                profileStep  = profileStep,
                name         = name,
                age          = age,
                job          = job,
                edu          = edu,
                onNameChange = { name = it },
                onAgeChange  = { age = it },
                onJobChange  = { job = it },
                onEduChange  = { edu = it },
                canNext      = canNext,
                isLastStep   = profileStep == TOTAL_PROFILE_STEPS - 1,
                onNext       = { handleNext() },
                onBack       = { step-- },
            )
        }
    }
}

// ── Slide content ─────────────────────────────────────────────────────────────

@Composable
private fun SlideContent(
    slide:        Slide,
    currentIdx:   Int,
    total:        Int,
    onNext:       () -> Unit,
    onSwipeLeft:  () -> Unit,
    onSwipeRight: () -> Unit,
) {
    var dragStartX by remember { mutableFloatStateOf(0f) }

    var totalDrag by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(currentIdx) {
                detectHorizontalDragGestures(
                    onDragStart  = { offset -> dragStartX = offset.x; totalDrag = 0f },
                    onDragEnd    = {
                        if (totalDrag < -SWIPE_THRESHOLD_PX) onSwipeLeft()
                        else if (totalDrag > SWIPE_THRESHOLD_PX) onSwipeRight()
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragCancel = { totalDrag = 0f },
                )
            },
    ) {
        // Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp),
        ) {
            slide.icon()
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text       = slide.title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text       = slide.desc,
            fontSize   = 15.sp,
            color      = Color.White.copy(alpha = 0.8f),
            lineHeight = 24.sp,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        // Dot indicator — active dot widens to 24dp (exact from JSX)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            repeat(total) { i ->
                val width by animateDpAsState(
                    targetValue   = if (i == currentIdx) 24.dp else 8.dp,
                    animationSpec = tween(300),
                    label         = "dotWidth$i",
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .background(
                            color = if (i == currentIdx) Color.White else Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text     = "Swipe atau tekan Lanjut",
            fontSize = 12.sp,
            color    = Color.White.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(12.dp))

        OnboardingButton(
            text    = "Lanjut →",
            enabled = true,
            onClick = onNext,
        )
    }
}

// ── Profile form content ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileFormContent(
    profileStep:  Int,
    name:         String,
    age:          String,
    job:          String,
    edu:          String,
    onNameChange: (String) -> Unit,
    onAgeChange:  (String) -> Unit,
    onJobChange:  (String) -> Unit,
    onEduChange:  (String) -> Unit,
    canNext:      Boolean,
    isLastStep:   Boolean,
    onNext:       () -> Unit,
    onBack:       () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DompetKuLogo(size = 44.dp, color = Color.White)

        Spacer(Modifier.height(16.dp))

        // Progress bar for profile steps — active segment is wider (flex:2 vs flex:1 in JSX)
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(TOTAL_PROFILE_STEPS) { i ->
                val weight = if (i == profileStep) 2f else 1f
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(4.dp)
                        .background(
                            color = if (i <= profileStep) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Step content
        when (profileStep) {
            0 -> ProfileStep(
                title       = "Siapa namamu?",
                subtitle    = "Kami akan menyapamu dengan nama ini.",
                content     = {
                    FrostInput(
                        value         = name,
                        onValueChange = onNameChange,
                        placeholder   = "Nama kamu...",
                        keyboardType  = KeyboardType.Text,
                    )
                },
            )
            1 -> ProfileStep(
                title    = "Berapa umurmu?",
                subtitle = "Membantu kami menyesuaikan analisis keuangan.",
                content  = {
                    FrostInput(
                        value         = age,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) onAgeChange(it) },
                        placeholder   = "Contoh: 22",
                        keyboardType  = KeyboardType.Number,
                    )
                },
            )
            2 -> ProfileStep(
                title    = "Pekerjaanmu sekarang?",
                subtitle = "Untuk analisis gaya hidup yang akurat.",
                content  = {
                    FrostDropdown(
                        selected       = job,
                        options        = JOBS,
                        placeholder    = "Pilih pekerjaan...",
                        onSelect       = onJobChange,
                    )
                },
            )
            3 -> ProfileStep(
                title    = "Pendidikan terakhirmu?",
                subtitle = "Digunakan untuk rekomendasi karir yang relevan.",
                content  = {
                    FrostDropdown(
                        selected    = edu,
                        options     = EDUS,
                        placeholder = "Pilih pendidikan...",
                        onSelect    = onEduChange,
                    )
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        // Back + Next buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Back always shown in profile steps (step > 0 globally = always in profile)
            OnboardingButton(
                text      = "Kembali",
                enabled   = true,
                onClick   = onBack,
                modifier  = Modifier.weight(1f),
            )
            OnboardingButton(
                text     = if (isLastStep) "Mulai Sekarang →" else "Lanjut →",
                enabled  = canNext,
                onClick  = onNext,
                modifier = Modifier.weight(2f),
            )
        }
    }
}

@Composable
private fun ProfileStep(
    title:   String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Text(text = title,    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    Spacer(Modifier.height(8.dp))
    Text(text = subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
    Spacer(Modifier.height(20.dp))
    content()
}

// ── Frosted glass input (white 20% bg, white 30% border — from JSX) ───────────

@Composable
private fun FrostInput(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    keyboardType:  KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = {
            Text(placeholder, color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
        },
        textStyle = LocalTextStyle.current.copy(
            color      = Color.White,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine      = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color.White.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedContainerColor   = Color.White.copy(alpha = 0.2f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
            cursorColor = Color.White,
        ),
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    )
}

// ── Frosted glass dropdown ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrostDropdown(
    selected:    String,
    options:     List<String>,
    placeholder: String,
    onSelect:    (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = it },
        modifier         = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value         = selected.ifEmpty { placeholder },
            onValueChange = {},
            readOnly      = true,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle     = LocalTextStyle.current.copy(
                color      = if (selected.isEmpty()) Color.White.copy(alpha = 0.6f) else Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor    = Color.White.copy(alpha = 0.3f),
                focusedContainerColor   = Color.White.copy(alpha = 0.15f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                focusedTrailingIconColor   = Color.White,
                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
            ),
            shape    = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .padding(bottom = 16.dp),
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.background(CardWhite),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text    = { Text(option, fontSize = 14.sp, color = TextDark) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

// ── Shared onboarding button (frosted glass style from JSX) ──────────────────

@Composable
private fun OnboardingButton(
    text:     String,
    enabled:  Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Color.White.copy(alpha = 0.25f),
            contentColor           = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.1f),
            disabledContentColor   = Color.White.copy(alpha = 0.4f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        modifier  = modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
