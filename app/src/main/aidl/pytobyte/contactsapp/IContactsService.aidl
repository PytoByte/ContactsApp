package pytobyte.contactsapp;

import pytobyte.contactsapp.IContactsCallback;

interface IContactsService {
    void deleteDuplicateContacts(IContactsCallback callback);
}