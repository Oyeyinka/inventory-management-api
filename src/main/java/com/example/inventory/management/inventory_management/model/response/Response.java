package com.example.inventory.management.inventory_management.model.response;

import java.time.LocalDateTime;

public class Response<T> {

    private String status;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public Response(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public T getData() {
        return data;
    }
}

