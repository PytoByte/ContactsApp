package pytobyte.contactsapp.model

data class Contact(
    val id: String,
    val displayName: String,
    val phones: List<PhoneInfo> = emptyList(),
    val photoUri: String? = null
)
