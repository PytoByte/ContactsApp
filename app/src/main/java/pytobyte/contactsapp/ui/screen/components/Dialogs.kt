package pytobyte.contactsapp.ui.screen.components

import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pytobyte.contactsapp.R
import pytobyte.contactsapp.model.Contact

@Composable
fun PermissionExplanationDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.dialog_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dialog_button_dismiss))
            }
        }
    )
}

@Composable
fun ContactNumbersDialog(
    contact: Contact,
    onNumberSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val resources = LocalResources.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = contact.displayName) },
        text = {
            LazyColumn {
                items(contact.phones) { phone ->
                    val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        resources, phone.type, phone.label
                    ).toString()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNumberSelected(phone.normalizedNumber ?: phone.number) },
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = phone.number,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dialog_button_dismiss))
            }
        }
    )
}