package pytobyte.contactsapp.ui.screen

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import pytobyte.contactsapp.R
import pytobyte.contactsapp.model.Contact
import pytobyte.contactsapp.model.OperationStatus
import pytobyte.contactsapp.ui.screen.components.ContactNumbersDialog
import pytobyte.contactsapp.ui.screen.components.ContactsList
import pytobyte.contactsapp.ui.screen.components.PermissionExplanationDialog
import pytobyte.contactsapp.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val readPermissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val writePermissionState = rememberPermissionState(Manifest.permission.WRITE_CONTACTS)
    val callPermissionState = rememberPermissionState(Manifest.permission.CALL_PHONE)

    var showReadRationale by remember { mutableStateOf(true) }
    var showCallRationale by remember { mutableStateOf(false) }
    var showWriteRationale by remember { mutableStateOf(false) }

    var pendingCall by remember { mutableStateOf<Pair<Contact, String>?>(null) }
    var contactForNumbersDialog by remember { mutableStateOf<Contact?>(null) }
    var isDeleteActionPending by remember { mutableStateOf(false) }

    val groupedContacts by viewModel.contacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val operationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()

    val makeDirectCall: (String) -> Unit = { phoneNumber ->
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:${phoneNumber.trim()}".toUri()
        }
        context.startActivity(intent)
    }

    val checkCallPermissionAndExecute: (Contact, String) -> Unit = { contact, number ->
        if (callPermissionState.status.isGranted) {
            makeDirectCall(number)
        } else {
            pendingCall = Pair(contact, number)
            showCallRationale = true
        }
    }

    LaunchedEffect(callPermissionState.status.isGranted) {
        if (callPermissionState.status.isGranted) {
            pendingCall?.let { (_, number) ->
                makeDirectCall(number)
                pendingCall = null
            }
        }
    }

    LaunchedEffect(writePermissionState.status.isGranted) {
        if (writePermissionState.status.isGranted && isDeleteActionPending) {
            viewModel.deleteDuplicates()
            isDeleteActionPending = false
        }
    }

    LaunchedEffect(operationStatus) {
        operationStatus?.let { status ->
            val messageRes = when (status) {
                OperationStatus.SUCCESS -> R.string.status_duplicates_success
                OperationStatus.NOT_FOUND -> R.string.status_duplicates_not_found
                else -> R.string.status_duplicates_error
            }
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(readPermissionState.status.isGranted) {
        if (readPermissionState.status.isGranted) {
            viewModel.loadContacts()
            viewModel.registerObserver()
        }
    }

    if (readPermissionState.status.isGranted) {
        ContactsContent(
            modifier = modifier,
            isLoading = isLoading,
            error = error,
            groupedContacts = groupedContacts,
            onRetryClick = { viewModel.loadContacts() },
            onContactClick = { contact ->
                val targetPhone = contact.phones.firstOrNull { it.isPrimary } ?: contact.phones.firstOrNull()
                targetPhone?.let {
                    checkCallPermissionAndExecute(contact, it.normalizedNumber ?: it.number)
                }
            },
            onContactLongClick = { contact -> contactForNumbersDialog = contact },
            onDeleteDuplicatesClick = {
                if (writePermissionState.status.isGranted) {
                    viewModel.deleteDuplicates()
                } else {
                    isDeleteActionPending = true
                    showWriteRationale = true
                }
            }
        )
    } else {
        NoReadPermissionScreen(
            modifier = modifier,
            showRationale = showReadRationale,
            onGrantClick = { showReadRationale = true },
            onConfirmRationale = {
                showReadRationale = false
                readPermissionState.launchPermissionRequest()
            },
            onDismissRationale = { showReadRationale = false }
        )
    }

    contactForNumbersDialog?.let { contact ->
        ContactNumbersDialog(
            contact = contact,
            onNumberSelected = { number ->
                contactForNumbersDialog = null
                checkCallPermissionAndExecute(contact, number)
            },
            onDismiss = { contactForNumbersDialog = null }
        )
    }

    if (showCallRationale) {
        PermissionExplanationDialog(
            title = stringResource(id = R.string.permission_call_title),
            description = stringResource(id = R.string.permission_call_description),
            onConfirm = {
                showCallRationale = false
                callPermissionState.launchPermissionRequest()
            },
            onDismiss = {
                showCallRationale = false
                pendingCall = null
            }
        )
    }

    if (showWriteRationale) {
        PermissionExplanationDialog(
            title = stringResource(id = R.string.permission_write_title),
            description = stringResource(id = R.string.permission_write_description),
            onConfirm = {
                showWriteRationale = false
                writePermissionState.launchPermissionRequest()
            },
            onDismiss = {
                showWriteRationale = false
                isDeleteActionPending = false
            }
        )
    }
}

@Composable
private fun ContactsContent(
    isLoading: Boolean,
    error: String?,
    groupedContacts: Map<String, List<Contact>>,
    onRetryClick: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    onDeleteDuplicatesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        LoadingState(modifier)
    } else if (error != null) {
        ErrorState(error = error, onRetryClick = onRetryClick, modifier = modifier)
    } else {
        SuccessState(
            groupedContacts = groupedContacts,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onDeleteDuplicatesClick = onDeleteDuplicatesClick,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.contacts_error_format, error),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetryClick) {
                Text(text = stringResource(id = R.string.contacts_button_retry))
            }
        }
    }
}

@Composable
private fun SuccessState(
    groupedContacts: Map<String, List<Contact>>,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    onDeleteDuplicatesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ContactsList(
            groupedContacts = groupedContacts,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onDeleteDuplicatesClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = stringResource(id = R.string.btn_delete_duplicates))
        }
    }
}

@Composable
private fun NoReadPermissionScreen(
    showRationale: Boolean,
    onGrantClick: () -> Unit,
    onConfirmRationale: () -> Unit,
    onDismissRationale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onGrantClick) {
            Text(text = stringResource(id = R.string.contacts_button_grant_access))
        }
    }

    if (showRationale) {
        PermissionExplanationDialog(
            title = stringResource(id = R.string.permission_read_title),
            description = stringResource(id = R.string.permission_read_description),
            onConfirm = onConfirmRationale,
            onDismiss = onDismissRationale
        )
    }
}
