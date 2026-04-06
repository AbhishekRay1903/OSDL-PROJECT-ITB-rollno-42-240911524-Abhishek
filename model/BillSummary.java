package com.osdl.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillSummary(long id, LocalDateTime billDate, String guestName, String roomNumber,
                          BigDecimal total, PaymentMethod paymentMethod) {
}
