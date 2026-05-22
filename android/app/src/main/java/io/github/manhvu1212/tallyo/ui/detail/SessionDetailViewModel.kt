package io.github.manhvu1212.tallyo.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.SessionRepository
import io.github.manhvu1212.tallyo.domain.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionDetailViewModel(
    private val repository: SessionRepository,
    val sessionId: String,
) : ViewModel() {

    val session: StateFlow<Session?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteRound(roundId: String) {
        viewModelScope.launch { repository.deleteRound(sessionId, roundId) }
    }

    fun setPlayerResting(playerId: String, resting: Boolean) {
        viewModelScope.launch {
            val current = session.value ?: return@launch
            repository.setPlayerResting(current, playerId, resting)
        }
    }

    fun addPlayer(name: String, onDuplicate: () -> Unit) {
        viewModelScope.launch {
            val current = session.value ?: return@launch
            if (current.players.any { it.name == name.trim() }) {
                onDuplicate()
                return@launch
            }
            repository.addPlayer(current, name)
        }
    }

    fun addQuickScore(playerId: String, points: Int, note: String?) {
        viewModelScope.launch {
            repository.addQuickScore(sessionId, playerId, points, note)
        }
    }

    fun deleteQuickScore(eventId: String) {
        viewModelScope.launch {
            repository.deleteQuickScore(sessionId, eventId)
        }
    }

    fun editQuickScore(eventId: String, playerId: String, points: Int, note: String?) {
        viewModelScope.launch {
            repository.editQuickScore(sessionId, eventId, playerId, points, note)
        }
    }

    companion object {
        fun factory(repository: SessionRepository, sessionId: String) = viewModelFactory {
            initializer { SessionDetailViewModel(repository, sessionId) }
        }
    }
}
