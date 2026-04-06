package com.osdl.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Stay(long id, long guestId, String guestName, long roomId, String roomNumber,
                   RoomCategory roomCategory, BigDecimal nightlyRate, LocalDateTime checkIn,
                   LocalDateTime checkOut, StayStatus status) {

    @Override
    public String toString() {
        return guestName + " · Room " + roomNumber + " (#" + id + ")";
    }
}
