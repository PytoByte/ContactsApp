package pytobyte.contactsapp.repository

import android.content.Context
import android.database.ContentObserver
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pytobyte.contactsapp.model.Contact
import pytobyte.contactsapp.model.PhoneInfo

class ContactRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val resolver = context.contentResolver

    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>()
        val phonesMap = mutableMapOf<String, MutableList<PhoneInfo>>()
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIdx =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                contactsMap[id] = Contact(
                    id = id,
                    displayName = cursor.getString(nameIdx),
                    photoUri = cursor.getString(photoIdx)
                )
                phonesMap[id] = mutableListOf()
            }
        }

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
            ),
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )
            val numberIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val normalizedIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            )
            val typeIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.TYPE
            )
            val labelIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.LABEL
            )
            val primaryIdx = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
            )

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIdx)
                val number = cursor.getString(numberIdx)
                if (contactId in contactsMap && number.isNotBlank()) {
                    phonesMap[contactId]?.add(
                        PhoneInfo(
                            number = number,
                            normalizedNumber = cursor.getString(normalizedIdx)
                                ?.takeIf { it.isNotBlank() },
                            type = cursor.getInt(typeIdx),
                            label = cursor.getString(labelIdx),
                            isPrimary = cursor.getInt(primaryIdx) == 1
                        )
                    )
                }
            }
        }

        contactsMap.values.map { contact ->
            contact.copy(phones = phonesMap[contact.id]?.distinctBy { it.number } ?: emptyList())
        }.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        )
    }

    fun registerObserver(observer: ContentObserver) {
        resolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer
        )
    }
}