package com.zekid.contactnotifier.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zekid.contactnotifier.data.AppSettings
import com.zekid.contactnotifier.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    // Source of truth from DataStore
    val settings: StateFlow<AppSettings> = repository.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings("https://ntfy.sh", "", true, true, false)
        )

    // Local editing state for text fields to avoid lag
    var editableServerUrl by mutableStateOf("")
        private set

    var editableTopic by mutableStateOf("")
        private set

    // Track if there are unsaved changes
    var hasUnsavedChanges by mutableStateOf(false)
        private set

    // Simple event flow for showing save feedback
    private val _saveEvent = MutableSharedFlow<Unit>()
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    init {
        // Initialize editable state with current values from DataStore
        viewModelScope.launch {
            val current = repository.appSettingsFlow.first()
            editableServerUrl = current.ntfyServerUrl
            editableTopic = current.ntfyTopic
            hasUnsavedChanges = false
        }
    }

    fun updateEditableUrl(url: String) {
        editableServerUrl = url
        checkUnsavedChanges()
    }

    fun updateEditableTopic(topic: String) {
        editableTopic = topic
        checkUnsavedChanges()
    }

    private fun checkUnsavedChanges() {
        viewModelScope.launch {
            val current = repository.appSettingsFlow.first()
            hasUnsavedChanges = editableServerUrl != current.ntfyServerUrl || 
                               editableTopic != current.ntfyTopic
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            repository.updateNtfyServerUrl(editableServerUrl)
            repository.updateNtfyTopic(editableTopic)
            hasUnsavedChanges = false
            _saveEvent.emit(Unit)
        }
    }

    // Toggles can still save immediately as they don't suffer from "shifting character" issues
    fun updateCallNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCallNotificationsEnabled(enabled)
        }
    }

    fun updateSmsNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSmsNotificationsEnabled(enabled)
        }
    }

    fun updateNotificationListenerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationListenerEnabled(enabled)
        }
    }
}
