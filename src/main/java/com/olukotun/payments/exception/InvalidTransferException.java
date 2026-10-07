package com.olukotun.payments.exception;

public class InvalidTransferException extends RuntimeException{

    public InvalidTransferException(String message) {
        super(message);
    }
}
