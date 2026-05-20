package io.github.manhvu1212.tallyo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.SessionRepository
import io.github.manhvu1212.tallyo.domain.Session
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    repository: SessionRepository,
    sessionId: String,
) : ViewModel() {

    val session: StateFlow<Session?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(repository: SessionRepository, sessionId: String) = viewModelFactory {
            initializer { StatsViewModel(repository, sessionId) }
        }
    }
}
