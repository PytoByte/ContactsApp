package pytobyte.contactsapp.model

import android.graphics.Bitmap

data class Contact(
    val id: String,
    val displayName: String,
    val phones: List<PhoneInfo> = emptyList(),
    val photoUri: String? = null,
    val photoBitmap: Bitmap? = null
)
