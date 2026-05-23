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
import pytobyte.contactsapp.manager.ContactsServiceManager
import pytobyte.contactsapp.model.Contact
import pytobyte.contactsapp.model.OperationStatus
import pytobyte.contactsapp.repository.ContactRepository

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val contactsServiceManager: ContactsServiceManager
) : ViewModel() {
    private val _contacts = MutableStateFlow<Map<String, List<Contact>>>(emptyMap())
    val contacts: StateFlow<Map<String, List<Contact>>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _operationStatus = MutableStateFlow<OperationStatus?>(null)
    val operationStatus: StateFlow<OperationStatus?> = _operationStatus.asStateFlow()

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadContacts()
        }
    }

    init {
        contactsServiceManager.bindService()
    }

    fun registerObserver() {
        contactRepository.registerObserver(contactsObserver)
    }

    fun loadContacts() = viewModelScope.launch {
        _isLoading.value = true
        _error.value = null
        try {
            val rawContacts = contactRepository.getContacts()
            _contacts.value = groupContactsByAlphabet(rawContacts)
        } catch (e: Exception) {
            _error.value = e.localizedMessage
        } finally {
            _isLoading.value = false
        }
    }

    private fun groupContactsByAlphabet(list: List<Contact>): Map<String, List<Contact>> {
        return list
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            .groupBy { contact ->
                contact.displayName.firstOrNull()?.uppercase()?.takeIf { it.isNotBlank() } ?: "#"
            }
    }

    override fun onCleared() {
        super.onCleared()
        contactRepository.unregisterObserver(contactsObserver)
        contactsServiceManager.unbindService()
    }

    fun deleteDuplicates() {
        contactsServiceManager.removeDuplicates { status ->
            _operationStatus.value = status
            loadContacts()
        }
    }

    fun clearStatus() {
        _operationStatus.value = null
    }
}