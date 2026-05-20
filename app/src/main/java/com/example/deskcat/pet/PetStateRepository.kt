package com.example.deskcat.pet

import androidx.compose.ui.geometry.Offset
import com.example.deskcat.DesktopPetUiState
import com.example.deskcat.PetMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PetStateRepository {
    private val _uiState = MutableStateFlow(DesktopPetUiState())
    val uiState: StateFlow<DesktopPetUiState> = _uiState.asStateFlow()

    fun onStageReady(stageWidth: Float, stageHeight: Float) {
        val current = _uiState.value
        if (current.initialized || stageWidth <= 0f || stageHeight <= 0f) return

        val petX = (stageWidth * 0.5f) - 96f
        val petY = (stageHeight * 0.42f) - 96f
        _uiState.value = current.copy(
            position = Offset(petX.coerceAtLeast(24f), petY.coerceAtLeast(24f)),
            bounds = Offset(stageWidth, stageHeight),
            initialized = true,
        )
    }

    fun dragPet(deltaX: Float, deltaY: Float) {
        val current = _uiState.value
        val petSize = 192f
        val maxX = (current.bounds.x - petSize).coerceAtLeast(0f)
        val maxY = (current.bounds.y - petSize).coerceAtLeast(0f)

        _uiState.value = current.copy(
            position = Offset(
                (current.position.x + deltaX).coerceIn(0f, maxX),
                (current.position.y + deltaY).coerceIn(0f, maxY),
            ),
            mood = if (deltaX * deltaX + deltaY * deltaY > 400f) PetMood.Excited else current.mood,
            speech = if (deltaX * deltaX + deltaY * deltaY > 400f) "哇，带我去兜风！" else current.speech,
        )
    }

    fun pet() {
        updateStats(
            mood = PetMood.Happy,
            hungerDelta = -2,
            happinessDelta = 7,
            energyDelta = -1,
            speech = listOf(
                "呼噜呼噜，摸摸最舒服了。",
                "再摸一下，我就要打滚了。",
                "今天的快乐值已经拉满。",
            ).random(),
        )
    }

    fun feed() {
        updateStats(
            mood = PetMood.Chill,
            hungerDelta = 18,
            happinessDelta = 4,
            energyDelta = 2,
            speech = listOf(
                "啊呜，谢谢投喂！",
                "饱饱的，肚子咕噜声都停了。",
                "这份零食我给满分。",
            ).random(),
        )
    }

    fun play() {
        updateStats(
            mood = PetMood.Excited,
            hungerDelta = 4,
            happinessDelta = 12,
            energyDelta = -8,
            speech = listOf(
                "再来一次，我还能继续玩！",
                "冲冲冲，今天就要活力满满。",
                "好耶，最喜欢一起玩了。",
            ).random(),
        )
    }

    fun rest() {
        updateStats(
            mood = PetMood.Sleepy,
            hungerDelta = 1,
            happinessDelta = 2,
            energyDelta = 15,
            speech = "我先眯一会儿，充电中...",
        )
    }

    fun nudgeIdleState() {
        val current = _uiState.value
        val nextMood = when {
            current.energy < 35 -> PetMood.Sleepy
            current.happiness > 85 -> PetMood.Excited
            current.hunger > 70 -> PetMood.Hungry
            else -> PetMood.Chill
        }

        val nextSpeech = when (nextMood) {
            PetMood.Sleepy -> "有点困了，想找个角落躺一会儿。"
            PetMood.Chill -> "今天适合安静陪着你。"
            PetMood.Happy -> "状态不错，随时可以互动。"
            PetMood.Excited -> "我精神很好，想蹦蹦跳跳！"
            PetMood.Hungry -> "肚子空空，来点好吃的吗？"
        }

        _uiState.value = current.copy(
            mood = nextMood,
            hunger = (current.hunger + 1).coerceIn(0, 100),
            happiness = (current.happiness - 1).coerceIn(0, 100),
            energy = (current.energy - 2).coerceIn(0, 100),
            speech = nextSpeech,
        )
    }

    fun resetPosition() {
        val current = _uiState.value
        val petX = (current.bounds.x * 0.5f) - 96f
        val petY = (current.bounds.y * 0.42f) - 96f
        _uiState.value = current.copy(
            position = Offset(petX.coerceAtLeast(24f), petY.coerceAtLeast(24f)),
            speech = "回到中间啦。",
        )
    }

    private fun updateStats(
        mood: PetMood,
        hungerDelta: Int,
        happinessDelta: Int,
        energyDelta: Int,
        speech: String,
    ) {
        val current = _uiState.value
        _uiState.value = current.copy(
            mood = mood,
            hunger = (current.hunger + hungerDelta).coerceIn(0, 100),
            happiness = (current.happiness + happinessDelta).coerceIn(0, 100),
            energy = (current.energy + energyDelta).coerceIn(0, 100),
            petCount = current.petCount + 1,
            speech = speech,
        )
    }
}
