package io.github.manhvu1212.tallyo.ui.sessions

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

class SessionsListViewModel(
    private val repository: SessionRepository,
) : ViewModel() {

    val sessions: StateFlow<List<Session>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteSession(id) }
    }

    companion object {
        fun factory(repository: SessionRepository) = viewModelFactory {
            initializer { SessionsListViewModel(repository) }
        }
    }
}
