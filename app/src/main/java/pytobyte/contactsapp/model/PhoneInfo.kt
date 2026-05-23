package pytobyte.contactsapp.model

data class PhoneInfo(
    val number: String,
    val normalizedNumber: String? = null,
    val type: Int,
    val label: String?,
    val isPrimary: Boolean
)
