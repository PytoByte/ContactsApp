package pytobyte.contactsapp.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import pytobyte.contactsapp.IContactsCallback
import pytobyte.contactsapp.IContactsService
import pytobyte.contactsapp.service.DuplicateContactsService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsServiceManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var iContactsService: IContactsService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            iContactsService = IContactsService.Stub.asInterface(service)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            iContactsService = null
            isBound = false
        }
    }

    fun bindService() {
        if (!isBound) {
            val intent = Intent(context, DuplicateContactsService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
            iContactsService = null
        }
    }

    fun removeDuplicates(onResult: (Int) -> Unit) {
        if (!isBound || iContactsService == null) {
            onResult(DuplicateContactsService.STATUS_ERROR)
            return
        }

        try {
            iContactsService?.deleteDuplicateContacts(object : IContactsCallback.Stub() {
                override fun onOperationFinished(statusCode: Int) {
                    onResult(statusCode)
                }
            })
        } catch (_: Exception) {
            onResult(DuplicateContactsService.STATUS_ERROR)
        }
    }
}