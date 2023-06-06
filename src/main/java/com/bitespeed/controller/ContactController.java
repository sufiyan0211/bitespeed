package com.bitespeed.controller;

import com.bitespeed.dto.RequestDtoContact;
import com.bitespeed.dto.ResponseDtoContact;
import com.bitespeed.exception.ContactControllerException;
import com.bitespeed.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping("/identify")
    public ResponseEntity<ResponseDtoContact> identity(RequestDtoContact requestDtoContact) {
        ResponseDtoContact responseDtoContact = contactService.identify(requestDtoContact);
        return ResponseEntity.ok(responseDtoContact);
    }

    @ExceptionHandler({
            ContactService.ContactMissingFieldException.class
    })
    private ResponseEntity<ContactControllerException> handleException(Exception ex) {
        HttpStatus status = null;
        String message = "";
        if (ex instanceof ContactService.ContactMissingFieldException) {
            status = HttpStatus.PARTIAL_CONTENT;
            message = ex.getMessage();
        }

        ContactControllerException contactControllerException = ContactControllerException.builder()
                .message(message)
                .build();

        return ResponseEntity.status(status).body(contactControllerException);
    }


}
