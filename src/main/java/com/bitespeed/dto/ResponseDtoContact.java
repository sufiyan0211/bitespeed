package com.bitespeed.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResponseDtoContact {
    private int primaryContactId;
    private List<String> emails;
    private List<Integer> phoneNumbers;
    private List<Integer> secondaryContactIds;
}
