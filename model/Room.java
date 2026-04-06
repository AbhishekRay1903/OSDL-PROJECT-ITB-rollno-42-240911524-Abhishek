package com.osdl.model;

import java.math.BigDecimal;

public record Room(long id, String roomNumber, RoomCategory category, BigDecimal nightlyRate,
                   RoomStatus status) {

    @Override
    public String toString() {
        return "Room " + roomNumber + " (" + category + ") — " + nightlyRate + "/night · " + status;
    }
}
