package com.yuvaintern.blog.common;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, long id) {
        super(resource + " with id " + id + " was not found.");
    }
}
