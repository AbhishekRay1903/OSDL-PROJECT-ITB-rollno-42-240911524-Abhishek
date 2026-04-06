package com.osdl.dao;

import com.osdl.db.Database;
import com.osdl.model.RoomCategory;
import com.osdl.model.Stay;
import com.osdl.model.StayStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class StayDao {

    public List<Stay> findActiveStays() throws SQLException {
        String sql = """
                SELECT s.id, s.guest_id, g.name AS guest_name, s.room_id, r.room_number,
                       r.category, r.nightly_rate, s.check_in, s.check_out, s.status
                FROM stay s
                JOIN guest g ON g.id = s.guest_id
                JOIN room r ON r.id = s.room_id
                WHERE s.status = 'CHECKED_IN'
                ORDER BY s.check_in DESC
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Stay> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public void checkIn(long guestId, long roomId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String ins = "INSERT INTO stay (guest_id, room_id, status) VALUES (?, ?, 'CHECKED_IN')";
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setLong(1, guestId);
                    ps.setLong(2, roomId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE room SET status = 'OCCUPIED' WHERE id = ?")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
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

    private static Stay mapRow(ResultSet rs) throws SQLException {
        return new Stay(
                rs.getLong("id"),
                rs.getLong("guest_id"),
                rs.getString("guest_name"),
                rs.getLong("room_id"),
                rs.getString("room_number"),
                RoomCategory.valueOf(rs.getString("category")),
                rs.getBigDecimal("nightly_rate"),
                toLocalDateTime(rs.getTimestamp("check_in")),
                toLocalDateTime(rs.getTimestamp("check_out")),
                StayStatus.valueOf(rs.getString("status")));
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
