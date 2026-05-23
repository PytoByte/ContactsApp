package pytobyte.contactsapp.ui.screen.components

import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pytobyte.contactsapp.R
import pytobyte.contactsapp.model.Contact

@Composable
private fun EmptyAvatar(modifier: Modifier, groupLetter: String) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = groupLetter,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun Avatar(modifier: Modifier, groupLetter: String, contact: Contact) {
    if (contact.photoBitmap != null) {
        Image(
            bitmap = contact.photoBitmap.asImageBitmap(),
            contentDescription = contact.displayName,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        EmptyAvatar(modifier = modifier, groupLetter = groupLetter)
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    groupLetter: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = contact.phones.firstOrNull { it.isPrimary } ?: contact.phones.firstOrNull()
    val hasPhones = contact.phones.isNotEmpty()
    val resources = LocalResources.current

    val clickableModifier = if (hasPhones) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }

    Box(modifier = modifier.then(clickableModifier)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                groupLetter = groupLetter,
                contact = contact
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasPhones) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (primary != null) {
                    val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        resources, primary.type, primary.label
                    ).toString()

                    Text(
                        text = "${primary.normalizedNumber ?: primary.number.trim()} ($label)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.contacts_empty_list),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsList(
    groupedContacts: Map<String, List<Contact>>,
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groupedContacts.isEmpty()) {
        Box(modifier, Alignment.Center) {
            Text(text = stringResource(id = R.string.contacts_empty_list))
        }
        return
    }

    LazyColumn(modifier) {
        groupedContacts.forEach { (letter, contacts) ->
            stickyHeader(key = "header_$letter") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(items = contacts, key = { "contact_${it.id}" }) { contact ->
                ContactRow(
                    contact = contact,
                    groupLetter = letter,
                    onClick = { onContactClick(contact) },
                    onLongClick = { onContactLongClick(contact) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}