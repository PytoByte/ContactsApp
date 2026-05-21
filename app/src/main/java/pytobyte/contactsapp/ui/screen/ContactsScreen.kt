package pytobyte.contactsapp.ui.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import pytobyte.contactsapp.model.Contact
import pytobyte.contactsapp.ui.viewmodel.ContactsViewModel
import androidx.core.net.toUri
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

private fun String.firstForContact(): String {
    return this.firstOrNull()?.uppercase()?.takeIf { it.isNotBlank() } ?: "#"
}

private fun List<Contact>.groupByAlphabet(): Map<String, List<Contact>> {
    return this
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
        .groupBy { contact ->
            contact.displayName.firstForContact()
        }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestBox(modifier: Modifier = Modifier, permissionState: PermissionState) {
    Box(modifier, Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Для работы приложения необходимо дать доступ с списку контактов",
                textAlign = TextAlign.Center
            )
            Button(onClick = {
                permissionState.launchPermissionRequest()
            }) {
                Text("Дать разрешение")
            }
        }
    }
}

@Composable
private fun EmptyAvatar(modifier: Modifier, displayName: String) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(displayName.firstForContact(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Avatar(modifier: Modifier, displayName: String, uri: Uri?) {
    val painter = rememberAsyncImagePainter(model = uri)

    when (painter.state) {
        is AsyncImagePainter.State.Success -> Image(
            modifier = modifier,
            painter = painter,
            contentDescription = displayName,
            contentScale = ContentScale.Crop
        )

        else -> EmptyAvatar(modifier, displayName)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ContactRow(modifier: Modifier, contact: Contact) {
    val context = LocalContext.current
    val callPermissionState = rememberPermissionState(Manifest.permission.CALL_PHONE)
    val primary = contact.phones.firstOrNull { it.isPrimary } ?: contact.phones.firstOrNull()

    Box(modifier.clickable(primary != null) {
        if (callPermissionState.status.isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:${primary!!.normalizedNumber ?: primary.number.trim()}".toUri()
            }
            context.startActivity(intent)
        } else {
            callPermissionState.launchPermissionRequest()
        }
    }) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Avatar(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contact.displayName,
                contact.photoUri?.toUri()
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    contact.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        primary?.normalizedNumber ?: primary?.number?.trim() ?: "",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

            }
        }
    }
}

@Composable
private fun ContactsList(modifier: Modifier = Modifier, contacts: List<Contact>) {
    if (contacts.isEmpty()) {
        Box(modifier, Alignment.Center) {
            Text("Пустой список контактов")
        }
        return
    }

    val grouped = contacts.groupByAlphabet()
    val letters = grouped.keys.toList()

    LazyColumn(modifier) {
        letters.forEach { letter ->
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

            items(
                items = grouped[letter] ?: emptyList(),
                key = { "contact_${it.id}" }
            ) { contact ->
                ContactRow(modifier = Modifier.fillMaxWidth(), contact = contact)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val permissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    if (permissionState.status.isGranted) {
        LaunchedEffect(permissionState.status.isGranted) {
            if (permissionState.status.isGranted) {
                viewModel.loadContacts()
                viewModel.registerObserver()
            }
        }

        if (isLoading) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { viewModel.loadContacts() }) {
                        Text("Повторить")
                    }
                }
            }
        } else {
            ContactsList(modifier.fillMaxSize(), contacts)
        }
    } else {
        PermissionRequestBox(modifier.fillMaxSize(), permissionState)
    }
}
