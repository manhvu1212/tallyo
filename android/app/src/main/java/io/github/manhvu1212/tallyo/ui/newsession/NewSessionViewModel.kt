package io.github.manhvu1212.tallyo.ui.newsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.SessionRepository
import kotlinx.coroutines.launch

class NewSessionViewModel(
    private val repository: SessionRepository,
) : ViewModel() {

    fun create(
        name: String,
        playerNames: List<String>,
        zeroSum: Boolean,
        defaultName: String,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val id = repository.createSession(name, playerNames, zeroSum, defaultName)
            onCreated(id)
        }
    }

    companion object {
        fun factory(repository: SessionRepository) = viewModelFactory {
            initializer { NewSessionViewModel(repository) }
        }
    }
}
