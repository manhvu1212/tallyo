package io.github.manhvu1212.tallyo.ui.addround

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.SessionRepository
import io.github.manhvu1212.tallyo.domain.RoundScore
import io.github.manhvu1212.tallyo.domain.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddRoundViewModel(
    private val repository: SessionRepository,
    val sessionId: String,
    val roundId: String?,
) : ViewModel() {

    val session: StateFlow<Session?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveNew(scores: List<RoundScore>, note: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            val s = session.value ?: return@launch
            repository.addRound(s, scores, note)
            onDone()
        }
    }

    fun saveEdit(scores: List<RoundScore>, note: String?, onDone: () -> Unit) {
        val rid = roundId ?: return
        viewModelScope.launch {
            repository.updateRound(sessionId, rid, scores, note)
            onDone()
        }
    }

    companion object {
        fun factory(
            repository: SessionRepository,
            sessionId: String,
            roundId: String?,
        ) = viewModelFactory {
            initializer { AddRoundViewModel(repository, sessionId, roundId) }
        }
    }
}
