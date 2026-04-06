package com.osdl.model;

import java.math.BigDecimal;

public record HotelService(long id, String code, String name, BigDecimal unitPrice) {

    @Override
    public String toString() {
        return code + " — " + name + " @ " + unitPrice;
    }
}
