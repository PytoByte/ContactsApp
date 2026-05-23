package pytobyte.contactsapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pytobyte.contactsapp.IContactsCallback
import pytobyte.contactsapp.IContactsService
import pytobyte.contactsapp.model.OperationStatus
import pytobyte.contactsapp.repository.ContactRepository

@AndroidEntryPoint
class DuplicateContactsService : Service() {
    @Inject
    lateinit var repository: ContactRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val binder = object : IContactsService.Stub() {
        override fun deleteDuplicateContacts(callback: IContactsCallback?) {
            serviceScope.launch {
                try {
                    val status = repository.removeDuplicateContacts()

                    callback?.onOperationFinished(status.ordinal)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                } catch (_: Exception) {
                    callback?.onOperationFinished(OperationStatus.ERROR.ordinal)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}