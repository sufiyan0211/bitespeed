package com.bitespeed.model;

import lombok.Data;
import lombok.NonNull;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "Contact")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String phoneNumber;

    private String email;

    private int linkedId;

    private LinkPrecedence linkPrecedence;

    @NonNull
    private Timestamp createdAt;

    @NonNull
    private Timestamp updatedAt;

    @NonNull
    private Timestamp deletedAt;

    public Contact() {

    }
}
