package com.bitespeed.repository;

import com.bitespeed.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    public Optional<Contact> findContactById(int id);
    public Optional<Contact> findContactByEmail(String email);
    public Optional<Contact> findContactByPhoneNumber(int phoneNumber);
}
