package pytobyte.contactsapp.model

import android.provider.ContactsContract

data class PhoneInfo(
    val number: String,
    val normalizedNumber: String? = null,
    val type: Int,
    val label: String?,
    val isPrimary: Boolean
) {
    val typeLabel: String
        get() = when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Мобильный"
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Домашний"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Рабочий"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Факс"
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Пейджер"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> label?.takeIf { it.isNotBlank() } ?: "Другой"
            else -> "Неизвестный"
        }
}
