package com.dompetku.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.theme.*
import kotlinx.coroutines.delay

private data class QuizQuestion(val question: String, val answer: Int)

private val QUIZ_POOL = listOf(
    QuizQuestion("12 \u00d7 8",    96),
    QuizQuestion("144 \u00f7 12",  12),
    QuizQuestion("25 \u00d7 4",   100),
    QuizQuestion("17 + 28",   45),
    QuizQuestion("100 - 37",  63),
    QuizQuestion("9 \u00d7 9",     81),
    QuizQuestion("256 \u00f7 16",  16),
    QuizQuestion("33 + 49",   82),
    QuizQuestion("7 \u00d7 13",    91),
    QuizQuestion("200 - 64", 136),
    QuizQuestion("15 \u00d7 6",    90),
    QuizQuestion("108 \u00f7 9",   12),
    QuizQuestion("48 + 75",  123),
    QuizQuestion("11 \u00d7 11",  121),
    QuizQuestion("500 - 173",327),
    QuizQuestion("13 \u00d7 11",  143),
    QuizQuestion("84 \u00f7 7",    12),
    QuizQuestion("36 + 87",  123),
    QuizQuestion("250 - 88", 162),
    QuizQuestion("6 \u00d7 17",   102),
)

private const val ROUNDS     = 3
private const val FEEDBACK_MS = 1000L

@Composable
fun MiniGameScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var questions  by remember { mutableStateOf(QUIZ_POOL.shuffled().take(ROUNDS)) }
    var round      by remember { mutableIntStateOf(0) }
    var input      by remember { mutableStateOf("") }
    var score      by remember { mutableIntStateOf(0) }
    var results    by remember { mutableStateOf(List<Boolean?>(ROUNDS) { null }) }
    var feedback   by remember { mutableStateOf<Boolean?>(null) }
    var done       by remember { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(FEEDBACK_MS)
            if (round + 1 >= ROUNDS) { done = true }
            else { round++; input = ""; feedback = null }
        }
    }

    fun submit() {
        if (input.isBlank() || feedback != null) return
        val correct = input.trim().toIntOrNull() == questions[round].answer
        if (correct) score++
        results  = results.toMutableList().also { it[round] = correct }
        feedback = correct
    }

    fun restart() {
        questions = QUIZ_POOL.shuffled().take(ROUNDS)
        round     = 0; input = ""; score = 0
        results   = List(ROUNDS) { null }
        feedback  = null; done = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(GreenDark, GreenPrimary))),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(16.dp).size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onBack)
                .align(Alignment.TopStart)
                .statusBarsPadding(),
        ) {
            Icon(PhosphorIcons.Regular.X, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        AnimatedContent(
            targetState = done,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.fillMaxSize(),
            label    = "game_state",
        ) { isDone ->
            if (isDone) {
                ResultScreen(score = score, results = results, onReplay = ::restart, onBack = onBack)
            } else {
                QuizScreen(
                    questions = questions, round = round, input = input,
                    feedback  = feedback, results = results,
                    onKey = { key ->
                        when (key) {
                            "\u232b" -> if (input.isNotEmpty()) input = input.dropLast(1)
                            "\u2713" -> submit()
                            else     -> if (input.length < 6) input += key
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuizScreen(
    questions: List<QuizQuestion>,
    round: Int,
    input: String,
    feedback: Boolean?,
    results: List<Boolean?>,
    onKey: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(ROUNDS) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == round) 12.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                results[i] == true  -> Color.White
                                results[i] == false -> Color.White.copy(alpha = 0.35f)
                                i == round          -> Color.White
                                else                -> Color.White.copy(alpha = 0.3f)
                            }
                        ),
                )
            }
        }

        Text(
            "Soal ${round + 1} dari $ROUNDS",
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color    = Color.White.copy(alpha = 0.75f),
        )

        Text(
            text       = "${questions[round].question} = ?",
            fontSize   = 42.sp, fontWeight = FontWeight.Black,
            color      = Color.White, textAlign = TextAlign.Center,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth().height(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    when (feedback) {
                        true  -> Color(0xFF22C55E).copy(alpha = 0.3f)
                        false -> Color(0xFFEF4444).copy(alpha = 0.3f)
                        null  -> Color.White.copy(alpha = 0.15f)
                    }
                ),
        ) {
            Text(
                text       = if (input.isEmpty()) "\u2014" else input,
                fontSize   = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            )
        }

        AnimatedVisibility(visible = feedback != null, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
            feedback?.let { correct ->
                Text(
                    text       = if (correct) "\u2705  Benar!" else "\u274c  Salah! Jawaban: ${questions[round].answer}",
                    fontSize   = 14.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
            }
        }

        CustomNumpad(enabled = feedback == null, onKey = onKey)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CustomNumpad(enabled: Boolean, onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
        listOf("\u232b","0","\u2713"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val isSubmit = key == "\u2713"
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f).aspectRatio(1.4f)
                            .clip(CircleShape)
                            .background(if (isSubmit) Color.White else Color.White.copy(alpha = 0.15f))
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onKey(key) },
                    ) {
                        when (key) {
                            "\u232b" -> Icon(PhosphorIcons.Regular.Backspace, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            "\u2713" -> Icon(PhosphorIcons.Regular.Check, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                            else     -> Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(
    score: Int, results: List<Boolean?>, onReplay: () -> Unit, onBack: () -> Unit
) {
    val perfect  = score == ROUNDS
    val title    = when (score) { ROUNDS -> "Sempurna! \uD83C\uDFAF"; ROUNDS-1 -> "Hampir! \uD83D\uDCAA"; else -> "Terus Berlatih!" }
    val subtitle = when (score) { ROUNDS -> "Kamu jenius matematika keuangan!"; ROUNDS-1 -> "Satu lagi dan sempurna!"; else -> "Jangan menyerah, coba lagi!" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 40.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
        ) {
            Icon(
                imageVector = if (perfect) PhosphorIcons.Regular.Trophy else PhosphorIcons.Regular.Calculator,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(52.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            results.forEach { correct ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                ) {
                    Text(
                        text       = when (correct) { true -> "\u2713"; false -> "\u2717"; else -> "?" },
                        fontSize   = 18.sp, fontWeight = FontWeight.ExtraBold,
                        color      = when (correct) { true -> Color.White; false -> Color.White.copy(alpha = 0.45f); else -> Color.White.copy(alpha = 0.3f) },
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.18f)).clickable(onClick = onReplay),
            ) {
                Text("Main Lagi", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp))
                    .background(Color.White).clickable(onClick = onBack),
            ) {
                Text("Selesai", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
            }
        }
    }
}
