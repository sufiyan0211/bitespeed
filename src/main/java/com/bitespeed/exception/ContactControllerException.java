package com.bitespeed.exception;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactControllerException {
    private String message;
}
