package com.osdl.dao;

import com.osdl.db.Database;
import com.osdl.model.HotelService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class HotelServiceDao {

    public List<HotelService> findAll() throws SQLException {
        String sql = "SELECT id, code, name, unit_price FROM hotel_service ORDER BY code";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<HotelService> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /** Extras shown in billing combo (ROOM nights added via separate button). */
    public List<HotelService> findBillableExtras() throws SQLException {
        String sql = """
                SELECT id, code, name, unit_price FROM hotel_service
                WHERE code <> 'ROOM' ORDER BY code
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<HotelService> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public long findIdByCode(String code) throws SQLException {
        String sql = "SELECT id FROM hotel_service WHERE code = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Missing hotel_service code: " + code);
                }
                return rs.getLong("id");
            }
        }
    }

    public long insert(String code, String name, BigDecimal unitPrice) throws SQLException {
        String sql = "INSERT INTO hotel_service (code, name, unit_price) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setBigDecimal(3, unitPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static HotelService mapRow(ResultSet rs) throws SQLException {
        return new HotelService(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getBigDecimal("unit_price"));
    }
}
