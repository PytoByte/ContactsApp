package pytobyte.contactsapp.service

import android.app.Service
import android.content.ContentProviderOperation
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pytobyte.contactsapp.IContactsCallback
import pytobyte.contactsapp.IContactsService

class DuplicateContactsService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val STATUS_SUCCESS = 1
        const val STATUS_ERROR = 2
        const val STATUS_NOT_FOUND = 3
    }

    private val binder = object : IContactsService.Stub() {
        override fun deleteDuplicateContacts(callback: IContactsCallback?) {
            serviceScope.launch {
                try {
                    val result = performDuplicateRemoval()
                    callback?.onOperationFinished(result)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    callback?.onOperationFinished(STATUS_ERROR)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun performDuplicateRemoval(): Int {
        val resolver = contentResolver
        val phoneMap = mutableMapOf<String, String>()
        val contactIdsToDelete = mutableListOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // 1. Ищем дубликаты по номерам телефонов
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIdx)
                val rawNumber = cursor.getString(numIdx) ?: continue
                val normalizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")

                if (phoneMap.containsKey(normalizedNumber)) {
                    val originalId = phoneMap[normalizedNumber]
                    if (originalId != contactId && !contactIdsToDelete.contains(contactId)) {
                        contactIdsToDelete.add(contactId)
                    }
                } else {
                    phoneMap[normalizedNumber] = contactId
                }
            }
        }

        if (contactIdsToDelete.isEmpty()) {
            return STATUS_NOT_FOUND
        }

        val operations = ArrayList<ContentProviderOperation>()
        for (contactId in contactIdsToDelete) {
            operations.add(
                ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(contactId))
                    .build()
            )
        }

        return try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operations)
            STATUS_SUCCESS
        } catch (e: Exception) {
            STATUS_ERROR
        }
    }
}