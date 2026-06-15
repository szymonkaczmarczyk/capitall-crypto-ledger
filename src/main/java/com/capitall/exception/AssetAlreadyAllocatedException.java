package com.capitall.exception;

public class AssetAlreadyAllocatedException extends RuntimeException {
    public AssetAlreadyAllocatedException(String message) {
        super(message);
    }
}
