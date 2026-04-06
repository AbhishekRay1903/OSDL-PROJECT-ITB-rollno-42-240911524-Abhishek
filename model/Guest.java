package com.osdl.model;

public record Guest(long id, String name, String phone) {

    @Override
    public String toString() {
        return name + (phone != null && !phone.isBlank() ? " · " + phone : "");
    }
}
