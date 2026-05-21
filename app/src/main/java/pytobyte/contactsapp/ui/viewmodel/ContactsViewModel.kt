package pytobyte.contactsapp.ui.viewmodel

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pytobyte.contactsapp.model.Contact
import pytobyte.contactsapp.repository.ContactRepository

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun registerObserver() {
        contactRepository.registerObserver(
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    viewModelScope.launch {
                        loadContacts()
                    }
                }
            }
        )
    }

    fun loadContacts() = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            _contacts.value = contactRepository.getContacts()
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Не удалось загрузить контакты"
        } finally {
            _isLoading.value = false
        }
    }
}
