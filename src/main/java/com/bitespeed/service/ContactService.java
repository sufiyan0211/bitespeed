package com.bitespeed.service;

import com.bitespeed.dto.RequestDtoContact;
import com.bitespeed.model.Contact;
import com.bitespeed.model.LinkPrecedence;
import com.bitespeed.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
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
     *
     * @param requestDtoContact
     * @return newly created contact
     */
    public Contact identify(RequestDtoContact requestDtoContact) {
        if ((requestDtoContact.getEmail() == null || requestDtoContact.getEmail().isEmpty()) &&
                (requestDtoContact.getPhoneNumber() == null || requestDtoContact.getPhoneNumber().isEmpty())) {
            throw new ContactMissingFieldException();
        }
        Contact contact = new Contact();
        contact.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        contact.setUpdatedAt(new Timestamp(System.currentTimeMillis()));


        int primaryAccountId = -1;

        if (requestDtoContact.getPhoneNumber() != null || !requestDtoContact.getPhoneNumber().isEmpty()) {
            String phoneNumber = requestDtoContact.getPhoneNumber();
            contact.setPhoneNumber(phoneNumber);
            primaryAccountId = findPrimaryAccountIdByPhone(phoneNumber);
        }
        else if(requestDtoContact.getEmail() != null || !requestDtoContact.getEmail().isEmpty()) {
            String email = requestDtoContact.getEmail();
            contact.setEmail(email);
            primaryAccountId = findPrimaryAccountIdByEmail(email);
        }

        if(primaryAccountId != -1) {
            contact.setLinkedId(primaryAccountId);
            contact.setLinkPrecedence(LinkPrecedence.secondary);
        }
        else {
            contact.setLinkPrecedence(LinkPrecedence.primary);
        }
        contactRepository.save(contact);
        return contact;
    }

    /**
     *
     * @param phoneNumber
     * @return ContactId of Primary account
     */
    int findPrimaryAccountIdByPhone(String phoneNumber) {
        List<Contact> contacts = contactRepository.findAll();
        for(Contact contact: contacts) {
            if(contact.getLinkPrecedence() == LinkPrecedence.primary &&
                    contact.getPhoneNumber() == phoneNumber){
                return contact.getId();
            }
        }
        return -1;
    }

    /**
     *
     * @param email
     * @return contactId of Primary account
     */
    int findPrimaryAccountIdByEmail(String email) {
        List<Contact> contacts = contactRepository.findAll();
        for(Contact contact: contacts) {
            if(contact.getLinkPrecedence() == LinkPrecedence.primary &&
                    contact.getEmail() == email){
                return contact.getId();
            }
        }
        return -1;
    }

    public static class ContactMissingFieldException extends IllegalArgumentException {
        public ContactMissingFieldException() {
            super("Either of email or phoneNumber fields needs to be fill");
        }
    }

}
