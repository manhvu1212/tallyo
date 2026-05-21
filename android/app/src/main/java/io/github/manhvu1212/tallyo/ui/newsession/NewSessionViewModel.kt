package io.github.manhvu1212.tallyo.ui.newsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.SessionRepository
import io.github.manhvu1212.tallyo.domain.CustomGame
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewSessionViewModel(
    private val repository: SessionRepository,
) : ViewModel() {

    val customGames: StateFlow<List<CustomGame>> = repository.observeCustomGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(
        name: String,
        game: String,
        playerNames: List<String>,
        zeroSum: Boolean,
        defaultName: String,
        isCustomGame: Boolean,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            if (isCustomGame) {
                repository.saveCustomGame(game, zeroSum)
            }
            val id = repository.createSession(name, game, playerNames, zeroSum, defaultName)
            onCreated(id)
        }
    }

    fun deleteCustomGame(name: String) {
        viewModelScope.launch {
            repository.deleteCustomGame(name)
        }
    }

    companion object {
        fun factory(repository: SessionRepository) = viewModelFactory {
            initializer { NewSessionViewModel(repository) }
        }
    }
}
