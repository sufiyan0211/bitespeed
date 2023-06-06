package com.bitespeed.service;

import com.bitespeed.dto.RequestDtoContact;
import com.bitespeed.dto.ResponseDtoContact;
import com.bitespeed.model.Contact;
import com.bitespeed.model.LinkPrecedence;
import com.bitespeed.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * <h2>service class for Contact related operation</h2>
 *
 * @author: sufiyan0211
 */
@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    /**
     * @param requestDtoContact
     * @return responseDtoContact
     */
    public ResponseDtoContact identify(RequestDtoContact requestDtoContact) {
        if ((requestDtoContact.getEmail() == null || requestDtoContact.getEmail().isEmpty()) &&
                (requestDtoContact.getPhoneNumber() == null || requestDtoContact.getPhoneNumber().isEmpty())) {
            throw new ContactMissingFieldException();
        }
        Contact contact = new Contact();
        contact.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        contact.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        int primaryAccountId = -1;
        int primaryAccountIdByEmail = -1;
        int primaryAccountIdByPhone = -1;
        Deque<Contact> linkContactsWithEmail = null;
        Deque<Contact> linkContactsWithPhone = null;

        if (requestDtoContact.getEmail() != null || !requestDtoContact.getEmail().isEmpty()) {
            String email = requestDtoContact.getEmail();
            contact.setEmail(email);
            linkContactsWithEmail = findLinkedContactByEmail(email);
            if (!linkContactsWithEmail.isEmpty() &&
                    linkContactsWithEmail.getFirst().getLinkPrecedence() == LinkPrecedence.primary) {
                primaryAccountIdByEmail = linkContactsWithEmail.getFirst().getId();
            }
        }

        if (requestDtoContact.getPhoneNumber() != null || !requestDtoContact.getPhoneNumber().isEmpty()) {
            String phoneNumber = requestDtoContact.getPhoneNumber();
            contact.setPhoneNumber(phoneNumber);
            linkContactsWithPhone = findLinkedContactByEmail(phoneNumber);
            if (!linkContactsWithPhone.isEmpty() &&
                    linkContactsWithPhone.getFirst().getLinkPrecedence() == LinkPrecedence.primary) {
                primaryAccountIdByPhone = linkContactsWithPhone.getFirst().getId();
            }
        }

        /**
         * @condition Where two primary accounts exist one is attached to Email and
         * other one is attached to PhoneNumber.
         *
         * @then: Final primaryContact would be the contact which is having same email and
         * other contact(primaryContact which is having same PhoneNumber) would be changed to secondary account.
         */
        boolean updatePrimaryAccountToSecondary = false;
        if (primaryAccountIdByEmail != -1 && primaryAccountIdByPhone != -1) {
            updatePrimaryAccountToSecondary = true;
            primaryAccountId = primaryAccountIdByEmail;
            updatePrimaryToSecondary(linkContactsWithPhone.getFirst(), primaryAccountIdByEmail);
        } else if (primaryAccountIdByEmail != -1) {
            primaryAccountId = primaryAccountIdByEmail;
        } else if (primaryAccountIdByPhone != -1) {
            primaryAccountId = primaryAccountIdByPhone;
        }


        if (primaryAccountId != -1 && !updatePrimaryAccountToSecondary) {
            contact.setLinkedId(primaryAccountId);
            contact.setLinkPrecedence(LinkPrecedence.secondary);
            contactRepository.save(contact);
        } else if (!updatePrimaryAccountToSecondary) {
            contact.setLinkPrecedence(LinkPrecedence.primary);
            contactRepository.save(contact);
        }

        return createResponseDtoContact(contact, updatePrimaryAccountToSecondary,
                linkContactsWithEmail, linkContactsWithPhone);
    }

    void updatePrimaryToSecondary(Contact oldPrimaryContact, int linkPrimaryContactId) {
        contactRepository.deleteById(oldPrimaryContact.getId());
        oldPrimaryContact.setLinkPrecedence(LinkPrecedence.secondary);
        oldPrimaryContact.setLinkedId(linkPrimaryContactId);
        oldPrimaryContact.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        contactRepository.save(oldPrimaryContact);
    }

    /**
     * @param phoneNumber
     * @return DoubleEnded Queue in which first element is Primary contact if exist and
     * other entries are Secondary Contacts
     */
    Deque<Contact> findLinkedContactByPhone(String phoneNumber) {
        Deque<Contact> linkContacts = new ArrayDeque<>();
        List<Contact> contacts = contactRepository.findAll();
        for (Contact contact : contacts) {
            if (contact.getPhoneNumber() == phoneNumber) {
                if (contact.getLinkPrecedence() == LinkPrecedence.primary) {
                    linkContacts.addFirst(contact); // add primary contact at first
                }
                linkContacts.addLast(contact); // add secondary contact at last
            }
        }
        return linkContacts;
    }

    /**
     * @param email
     * @return DoubleEnded Queue in which first element is Primary contact if exist and
     * other entries are Secondary Contacts
     */
    Deque<Contact> findLinkedContactByEmail(String email) {
        Deque<Contact> linkContacts = new ArrayDeque<>();
        List<Contact> contacts = contactRepository.findAll();
        for (Contact contact : contacts) {
            if (contact.getEmail() == email) {
                if (contact.getLinkPrecedence() == LinkPrecedence.primary) {
                    linkContacts.addFirst(contact); // add primary contact at first
                }
                linkContacts.addLast(contact); // add secondary contact at last
            }
        }
        return linkContacts;
    }

    /**
     * @param newContact
     * @param updatePrimaryAccountToSecondary
     * @param linkContactsWithEmail
     * @param linkContactsWithPhone
     * @return
     */
    private ResponseDtoContact createResponseDtoContact(Contact newContact, boolean updatePrimaryAccountToSecondary,
                                                        Deque<Contact> linkContactsWithEmail,
                                                        Deque<Contact> linkContactsWithPhone) {
        ResponseDtoContact responseDtoContact = new ResponseDtoContact();
        List<String> emails = new ArrayList<>();
        List<String> phoneNumbers = new ArrayList<>();
        List<Integer> secondaryContactIds = new ArrayList<>();

        if (updatePrimaryAccountToSecondary) {
            responseDtoContact.setPrimaryContactId(linkContactsWithEmail.getFirst().getId());

            emails.add(linkContactsWithEmail.getFirst().getEmail());
            phoneNumbers.add(linkContactsWithEmail.getFirst().getPhoneNumber());

            responseDtoContact.setEmails(emails);
            responseDtoContact.setPhoneNumbers(phoneNumbers);
            responseDtoContact.setSecondaryContactIds(secondaryContactIds);

            responseDtoContact = fillSecondaryContacts(linkContactsWithEmail, responseDtoContact);
            responseDtoContact = fillSecondaryContacts(linkContactsWithPhone, responseDtoContact);

            return responseDtoContact;
        }
        // if newly created contact is Primary contact then
        if (newContact.getLinkPrecedence() == LinkPrecedence.primary) {
            responseDtoContact.setPrimaryContactId(newContact.getId());
            if (newContact.getEmail() != null && !newContact.getEmail().isEmpty()) {
                emails.add(newContact.getEmail());
            }
            if (newContact.getPhoneNumber() != null && !newContact.getPhoneNumber().isEmpty()) {
                phoneNumbers.add(newContact.getPhoneNumber());
            }
            return responseDtoContact;
        }

        // if newly created contact is secondary contact then
        if (newContact.getLinkPrecedence() == LinkPrecedence.secondary) {
            responseDtoContact.setPrimaryContactId(newContact.getLinkedId());
            if (linkContactsWithEmail.getFirst().getLinkPrecedence() == LinkPrecedence.primary &&
                    linkContactsWithEmail.getFirst().getId() == newContact.getLinkedId()) {
                emails.add(linkContactsWithEmail.getFirst().getEmail());
                phoneNumbers.add(linkContactsWithEmail.getFirst().getPhoneNumber());
            } else if (linkContactsWithPhone.getFirst().getLinkPrecedence() == LinkPrecedence.primary &&
                    linkContactsWithPhone.getFirst().getId() == newContact.getLinkedId()) {
                emails.add(linkContactsWithPhone.getFirst().getEmail());
                phoneNumbers.add(linkContactsWithPhone.getFirst().getPhoneNumber());
            }
        }
        responseDtoContact.setEmails(emails);
        responseDtoContact.setPhoneNumbers(phoneNumbers);
        responseDtoContact.setSecondaryContactIds(secondaryContactIds);

        responseDtoContact = fillSecondaryContacts(linkContactsWithEmail, responseDtoContact);
        responseDtoContact = fillSecondaryContacts(linkContactsWithPhone, responseDtoContact);

        return responseDtoContact;
    }


    /**
     * @param linkContacts
     * @param responseDtoContact
     * @param responseDtoContact
     * @return
     */
    ResponseDtoContact fillSecondaryContacts(Deque<Contact> linkContacts, ResponseDtoContact responseDtoContact) {

        List<String> emails = responseDtoContact.getEmails();
        List<String> phoneNumbers = responseDtoContact.getPhoneNumbers();
        List<Integer> secondaryContactIds = responseDtoContact.getSecondaryContactIds();

        for (Contact contact : linkContacts) {
            if (contact.getLinkPrecedence() == LinkPrecedence.secondary) {
                emails.add(contact.getEmail());
                phoneNumbers.add(contact.getPhoneNumber());
                secondaryContactIds.add(contact.getId());
            }
        }
        responseDtoContact.setEmails(emails);
        responseDtoContact.setPhoneNumbers(phoneNumbers);
        responseDtoContact.setSecondaryContactIds(secondaryContactIds);

        return responseDtoContact;
    }

    public static class ContactMissingFieldException extends IllegalArgumentException {
        public ContactMissingFieldException() {
            super("Either of email or phoneNumber fields needs to be fill");
        }
    }

}
