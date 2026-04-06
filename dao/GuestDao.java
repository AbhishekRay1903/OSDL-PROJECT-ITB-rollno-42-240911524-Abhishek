package com.osdl.dao;

import com.osdl.db.Database;
import com.osdl.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class GuestDao {

    public List<Guest> findAll() throws SQLException {
        String sql = "SELECT id, name, phone FROM guest ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Guest> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Guest(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("phone")));
            }
            return list;
        }
    }

    public long insert(String name, String phone) throws SQLException {
        String sql = "INSERT INTO guest (name, phone) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone == null || phone.isBlank() ? null : phone);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }
}
