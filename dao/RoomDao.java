package com.osdl.dao;

import com.osdl.db.Database;
import com.osdl.model.Room;
import com.osdl.model.RoomCategory;
import com.osdl.model.RoomStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class RoomDao {

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT id, room_number, category, nightly_rate, status FROM room ORDER BY room_number";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Room> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public List<Room> findAvailable() throws SQLException {
        String sql = """
                SELECT id, room_number, category, nightly_rate, status FROM room
                WHERE status = 'AVAILABLE' ORDER BY room_number
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Room> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public long insert(String roomNumber, RoomCategory category, BigDecimal nightlyRate) throws SQLException {
        String sql = "INSERT INTO room (room_number, category, nightly_rate, status) VALUES (?, ?, ?, 'AVAILABLE')";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomNumber);
            ps.setString(2, category.name());
            ps.setBigDecimal(3, nightlyRate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateStatus(long roomId, RoomStatus status) throws SQLException {
        String sql = "UPDATE room SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, roomId);
            ps.executeUpdate();
        }
    }

    private static Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
                rs.getLong("id"),
                rs.getString("room_number"),
                RoomCategory.valueOf(rs.getString("category")),
                rs.getBigDecimal("nightly_rate"),
                RoomStatus.valueOf(rs.getString("status")));
    }
}
