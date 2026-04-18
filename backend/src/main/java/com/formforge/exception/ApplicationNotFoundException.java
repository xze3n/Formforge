package com.formforge.exception;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(Long id) {
        super("Application not found with id: " + id);
    }
}
