package pytobyte.contactsapp.repository

import android.content.Context
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pytobyte.contactsapp.di.IoDispatcher
import pytobyte.contactsapp.model.Contact
import pytobyte.contactsapp.model.PhoneInfo
import java.io.InputStream
import androidx.core.net.toUri

class ContactRepository @Inject constructor(
    @ApplicationContext context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val resolver = context.contentResolver

    suspend fun getContacts(): List<Contact> = withContext(ioDispatcher) {
        val contactsMap = mutableMapOf<String, Contact>()
        val phonesMap = mutableMapOf<String, MutableList<PhoneInfo>>()

        val contactProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        )

        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactProjection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                val photoUri = cursor.getString(photoIdx)
                val bitmap = photoUri?.let { uriStr ->
                    var inputStream: InputStream? = null
                    try {
                        inputStream = resolver.openInputStream(uriStr.toUri())
                        BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    } finally {
                        inputStream?.close()
                    }
                }
                contactsMap[id] = Contact(
                    id = id,
                    displayName = cursor.getString(nameIdx),
                    photoUri = photoUri,
                    photoBitmap = bitmap
                )
                phonesMap[id] = mutableListOf()
            }
        }

        val phoneProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
        )

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            phoneProjection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalizedIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            val primaryIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIdx)
                val number = cursor.getString(numberIdx)

                val phoneList = phonesMap[contactId]
                if (phoneList != null && number.isNotBlank()) {
                    if (phoneList.none { it.number == number }) {
                        phoneList.add(
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
        }

        contactsMap.values.map { contact ->
            contact.copy(phones = phonesMap[contact.id] ?: emptyList())
        }
    }

    fun registerObserver(observer: ContentObserver) {
        resolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer
        )
    }

    fun unregisterObserver(observer: ContentObserver) {
        resolver.unregisterContentObserver(observer)
    }
}