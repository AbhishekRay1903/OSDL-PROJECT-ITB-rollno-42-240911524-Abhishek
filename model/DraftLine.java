package com.osdl.model;

import java.math.BigDecimal;

public record DraftLine(long serviceId, String code, String itemName, int qty, BigDecimal unitPrice,
                        BigDecimal lineTotal) {
}
