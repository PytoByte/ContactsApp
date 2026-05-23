package pytobyte.contactsapp.repository

import android.content.ContentProviderOperation
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
import pytobyte.contactsapp.model.OperationStatus

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

    suspend fun removeDuplicateContacts(): OperationStatus = withContext(ioDispatcher) {
        try {
            val contactDataBundles = mutableMapOf<String, MutableSet<String>>()

            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE, ContactsContract.Data.DATA1),
                null, null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                val mimeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
                val data1Idx = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx) ?: continue
                    val mimeType = cursor.getString(mimeIdx) ?: continue
                    val rawData = cursor.getString(data1Idx) ?: continue

                    if (mimeType == ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE) continue

                    val normalizedData = if (mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) {
                        rawData.replace(Regex("[^0-9+]"), "")
                    } else {
                        rawData.trim().lowercase()
                    }

                    if (normalizedData.isNotBlank()) {
                        contactDataBundles.getOrPut(contactId) { mutableSetOf() }.add("$mimeType:$normalizedData")
                    }
                }
            }

            val contactIdsToDelete = contactDataBundles
                .filterValues { it.isNotEmpty() }
                .entries
                .groupBy { it.value.sorted() }
                .values
                .flatMap { group -> group.drop(1).map { it.key } }

            if (contactIdsToDelete.isEmpty()) {
                return@withContext OperationStatus.NOT_FOUND
            }

            val operations = contactIdsToDelete.mapTo(ArrayList()) { contactId ->
                ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(contactId))
                    .build()
            }

            resolver.applyBatch(ContactsContract.AUTHORITY, operations)
            OperationStatus.SUCCESS

        } catch (_: Exception) {
            OperationStatus.ERROR
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