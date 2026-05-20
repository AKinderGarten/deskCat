package com.example.deskcat

import androidx.lifecycle.ViewModel
import com.example.deskcat.pet.PetStateRepository
import kotlinx.coroutines.flow.StateFlow

class BakingViewModel : ViewModel() {
    val uiState: StateFlow<DesktopPetUiState> = PetStateRepository.uiState

    fun onStageReady(stageWidth: Float, stageHeight: Float) {
        PetStateRepository.onStageReady(stageWidth, stageHeight)
    }

    fun dragPet(deltaX: Float, deltaY: Float) {
        PetStateRepository.dragPet(deltaX, deltaY)
    }

    fun pet() {
        PetStateRepository.pet()
    }

    fun feed() {
        PetStateRepository.feed()
    }

    fun play() {
        PetStateRepository.play()
    }

    fun rest() {
        PetStateRepository.rest()
    }

    fun nudgeIdleState() {
        PetStateRepository.nudgeIdleState()
    }

    fun resetPosition() {
        PetStateRepository.resetPosition()
    }
}
