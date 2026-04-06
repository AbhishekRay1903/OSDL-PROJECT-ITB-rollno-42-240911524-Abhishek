package com.osdl.dao;

import com.osdl.db.Database;
import com.osdl.model.BillSummary;
import com.osdl.model.DraftLine;
import com.osdl.model.PaymentMethod;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class BillDao {

    public List<BillSummary> findAllSummaries() throws SQLException {
        String sql = """
                SELECT b.id, b.bill_date, g.name AS guest_name, r.room_number, b.total, b.payment_method
                FROM bill b
                JOIN stay s ON s.id = b.stay_id
                JOIN guest g ON g.id = s.guest_id
                JOIN room r ON r.id = s.room_id
                ORDER BY b.bill_date DESC, b.id DESC
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<BillSummary> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new BillSummary(
                        rs.getLong("id"),
                        toLocalDateTime(rs.getTimestamp("bill_date")),
                        rs.getString("guest_name"),
                        rs.getString("room_number"),
                        rs.getBigDecimal("total"),
                        PaymentMethod.valueOf(rs.getString("payment_method"))));
            }
            return list;
        }
    }

    /**
     * Saves bill lines, marks stay CHECKED_OUT, frees the room.
     */
    public void createBillForStay(long stayId, List<DraftLine> lines, PaymentMethod paymentMethod)
            throws SQLException {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Bill must have at least one line");
        }
        BigDecimal total = lines.stream()
                .map(DraftLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                verifyStayCheckedIn(conn, stayId);
                long roomId = fetchRoomIdForStay(conn, stayId);
                long billId = insertBill(conn, stayId, total, paymentMethod);
                insertLines(conn, billId, lines);
                checkoutStay(conn, stayId);
                freeRoom(conn, roomId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se) {
                    throw se;
                }
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static void verifyStayCheckedIn(Connection conn, long stayId) throws SQLException {
        String sql = "SELECT status FROM stay WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, stayId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Stay not found: " + stayId);
                }
                if (!"CHECKED_IN".equals(rs.getString("status"))) {
                    throw new SQLException("Stay is not active (already checked out?)");
                }
            }
        }
    }

    private static long fetchRoomIdForStay(Connection conn, long stayId) throws SQLException {
        String sql = "SELECT room_id FROM stay WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, stayId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("room_id");
            }
        }
    }

    private static long insertBill(Connection conn, long stayId, BigDecimal total,
                                   PaymentMethod paymentMethod) throws SQLException {
        String sql = "INSERT INTO bill (stay_id, total, payment_method) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, stayId);
            ps.setBigDecimal(2, total);
            ps.setString(3, paymentMethod.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void insertLines(Connection conn, long billId, List<DraftLine> lines)
            throws SQLException {
        String sql = "INSERT INTO bill_line (bill_id, hotel_service_id, qty, line_total) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DraftLine line : lines) {
                ps.setLong(1, billId);
                ps.setLong(2, line.serviceId());
                ps.setInt(3, line.qty());
                ps.setBigDecimal(4, line.lineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void checkoutStay(Connection conn, long stayId) throws SQLException {
        String sql = """
                UPDATE stay SET status = 'CHECKED_OUT', check_out = CURRENT_TIMESTAMP WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, stayId);
            ps.executeUpdate();
        }
    }

    private static void freeRoom(Connection conn, long roomId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE room SET status = 'AVAILABLE' WHERE id = ?")) {
            ps.setLong(1, roomId);
            ps.executeUpdate();
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
