package com.example.deskcat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel(),
    overlayGranted: Boolean,
    overlayRunning: Boolean,
    onOpenOverlayPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    val uiState by bakingViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val floatTransition = rememberInfiniteTransition(label = "petFloat")
    val floatOffset by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatOffset",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF7F4EE),
                        Color(0xFFEDE7DD),
                        Color(0xFFDCD5CA),
                    ),
                ),
            ),
    ) {
        val compact = maxWidth < 360.dp || maxHeight < 700.dp
        val pagePadding = if (compact) 14.dp else 18.dp
        val cardSpacing = if (compact) 10.dp else 14.dp
        val bobAmplitude = if (compact) 4f else 6f
        val stageHeight = if (compact) {
            250.dp
        } else {
            (maxHeight * 0.56f).coerceAtLeast(300.dp)
        }
        val density = LocalDensity.current
        val stageWidthPx = with(density) {
            (maxWidth - pagePadding * 2 - 24.dp).toPx().coerceAtLeast(0f)
        }
        val stageHeightPx = with(density) {
            (stageHeight - 24.dp).toPx().coerceAtLeast(0f)
        }

        LaunchedEffect(stageWidthPx, stageHeightPx) {
            bakingViewModel.onStageReady(stageWidthPx, stageHeightPx)
        }

        val petScale = when (uiState.mood) {
            PetMood.Sleepy -> 0.96f
            PetMood.Chill -> 1f
            PetMood.Happy -> 1.03f
            PetMood.Excited -> 1.07f
            PetMood.Hungry -> 0.99f
        }
        val petRotation = when (uiState.mood) {
            PetMood.Sleepy -> -2f
            PetMood.Chill -> 0f
            PetMood.Happy -> 2f
            PetMood.Excited -> 4f
            PetMood.Hungry -> -1f
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(pagePadding),
            verticalArrangement = Arrangement.spacedBy(cardSpacing),
        ) {
            HeaderCard(
                uiState = uiState,
                overlayGranted = overlayGranted,
                overlayRunning = overlayRunning,
                onOpenOverlayPermission = onOpenOverlayPermission,
                onStartOverlay = onStartOverlay,
                onStopOverlay = onStopOverlay,
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stageHeight),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x55FFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                ) {
                    BackgroundGlow()

                    Text(
                        text = "拖动小猫到处跑",
                        modifier = Modifier.align(Alignment.TopStart),
                        color = Color(0x99000000),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    if (uiState.initialized) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        uiState.position.x.roundToInt(),
                                        uiState.position.y.roundToInt(),
                                    )
                                }
                                .requiredSize(192.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        bakingViewModel.dragPet(dragAmount.x, dragAmount.y)
                                    }
                                },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedVisibility(visible = uiState.speech.isNotBlank()) {
                                    SpeechBubble(text = uiState.speech)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(176.dp)
                                        .offset(y = ((floatOffset - 0.5f) * bobAmplitude).dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PetAvatar(
                                        mood = uiState.mood,
                                        scale = petScale,
                                        rotation = petRotation,
                                        phase = floatOffset,
                                        modifier = Modifier.requiredSize(150.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ActionPanel(
                onPet = bakingViewModel::pet,
                onFeed = bakingViewModel::feed,
                onPlay = bakingViewModel::play,
                onRest = bakingViewModel::rest,
                onReset = bakingViewModel::resetPosition,
            )
        }
    }
}

@Composable
private fun HeaderCard(
    uiState: DesktopPetUiState,
    overlayGranted: Boolean,
    overlayRunning: Boolean,
    onOpenOverlayPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF)),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "桌宠喵",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    MoodBadge(mood = uiState.mood)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onOpenOverlayPermission,
                        label = { Text("设置") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (overlayGranted) Color(0xFFF1E7D7) else Color(0xFFE8E2D8),
                            labelColor = Color(0xFF111111),
                        ),
                    )
                    AssistChip(
                        onClick = if (overlayRunning) onStopOverlay else onStartOverlay,
                        label = { Text(if (overlayRunning) "关闭" else "开启") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFF7F4EE),
                            labelColor = Color(0xFF111111),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "拖动我到处走走，摸摸、喂食、玩耍、休息都会影响我的状态。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF444444),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "互动 ${uiState.petCount} 次",
                color = Color(0xFF666666),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatRow(label = "饱腹", value = uiState.hunger)
            StatRow(label = "开心", value = uiState.happiness)
            StatRow(label = "精力", value = uiState.energy)
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, color = Color(0xFF333333), fontSize = 13.sp)
            Text(text = "$value%", color = Color(0xFF333333), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0x33111111), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value / 100f)
                    .height(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1B1B1B), Color(0xFFF2F2F2)),
                        ),
                        RoundedCornerShape(999.dp),
                    ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun MoodBadge(mood: PetMood) {
    val (text, background) = when (mood) {
        PetMood.Sleepy -> "想睡觉" to Color(0xFFD7D7D7)
        PetMood.Chill -> "很放松" to Color(0xFFEAEAEA)
        PetMood.Happy -> "心情好" to Color(0xFFF6F6F6)
        PetMood.Excited -> "很兴奋" to Color(0xFFFFFFFF)
        PetMood.Hungry -> "有点饿" to Color(0xFFD1CDC5)
    }

    Surface(color = background, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = Color(0xFF111111),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.width(220.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = Color(0xFF111111),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActionPanel(
    onPet: () -> Unit,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onRest: () -> Unit,
    onReset: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF)),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "互动操作",
                color = Color(0xFF111111),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionChip(
                    text = "摸摸",
                    onClick = onPet,
                    containerColor = Color(0xFFF1E7D7),
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    text = "喂食",
                    onClick = onFeed,
                    containerColor = Color(0xFFE8E2D8),
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    text = "玩耍",
                    onClick = onPlay,
                    containerColor = Color(0xFFF7F4EE),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionChip(
                    text = "休息",
                    onClick = onRest,
                    containerColor = Color(0xFFE6E1DA),
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    text = "归位",
                    onClick = onReset,
                    containerColor = Color(0xFFF0EEE9),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = Color(0xFF111111),
        ),
    )
}

@Composable
private fun BackgroundGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0x20FFFFFF),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.16f, size.height * 0.16f),
        )
        drawCircle(
            color = Color(0x22FFFFFF),
            radius = size.minDimension * 0.26f,
            center = Offset(size.width * 0.82f, size.height * 0.18f),
        )
        drawCircle(
            color = Color(0x14FFFFFF),
            radius = size.minDimension * 0.2f,
            center = Offset(size.width * 0.82f, size.height * 0.8f),
        )
    }
}

@Composable
private fun PetAvatar(
    mood: PetMood,
    scale: Float,
    rotation: Float,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    val catFrame = when (mood) {
        PetMood.Chill -> R.drawable.cat5_re
        PetMood.Happy -> when {
            phase < 0.2f -> R.drawable.cat5_re
            phase < 0.4f -> R.drawable.cat6_re
            phase < 0.6f -> R.drawable.cat7_re
            phase < 0.8f -> R.drawable.cat6_re
            else -> R.drawable.cat5_re
        }
        PetMood.Sleepy -> when {
            phase < 0.5f -> R.drawable.cat6_re
            else -> R.drawable.cat7_re
        }
        PetMood.Excited -> when {
            phase < 0.25f -> R.drawable.cat5_re
            phase < 0.5f -> R.drawable.cat8_re
            phase < 0.75f -> R.drawable.cat9_re
            else -> R.drawable.cat5_re
        }
        PetMood.Hungry -> R.drawable.cat5_re
    }
    val wave = kotlin.math.sin(phase * kotlin.math.PI * 2.0).toFloat()
    val moodTilt = when (mood) {
        PetMood.Sleepy -> -4f
        PetMood.Chill -> 0f
        PetMood.Happy -> 0.8f
        PetMood.Excited -> 2f
        PetMood.Hungry -> -0.5f
    }
    val moodNudgeX = when (mood) {
        PetMood.Sleepy -> -0.8f
        PetMood.Chill -> 0f
        PetMood.Happy -> 0.8f
        PetMood.Excited -> 1.8f
        PetMood.Hungry -> -0.3f
    }
    val moodNudgeY = when (mood) {
        PetMood.Sleepy -> 2f
        PetMood.Chill -> 0f
        PetMood.Happy -> 1f
        PetMood.Excited -> -1.4f
        PetMood.Hungry -> 0.5f
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationX = moodNudgeX + wave * when (mood) {
                PetMood.Excited -> 2.4f
                PetMood.Happy -> 1.2f
                PetMood.Sleepy -> 0.8f
                else -> 1f
            }
            translationY = moodNudgeY + wave * when (mood) {
                PetMood.Excited -> 1.8f
                PetMood.Happy -> 1f
                PetMood.Sleepy -> 1.2f
                else -> 0.9f
            }
            rotationZ = rotation + moodTilt + wave * when (mood) {
                PetMood.Excited -> 1.3f
                PetMood.Happy -> 0.7f
                PetMood.Sleepy -> 0.6f
                else -> 0.5f
            }
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = catFrame),
            contentDescription = "桌宠小猫",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
