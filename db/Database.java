package com.osdl.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Embedded H2 file database. Data lives under ./data/ (created at runtime).
 */
public final class Database {

    private static final String JDBC_URL =
            "jdbc:h2:file:./data/osdl-store;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, "sa", "");
    }

    /** Runs classpath resource db/schema.sql once per application startup (idempotent DDL). */
    public static void initializeSchema() throws SQLException, IOException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            String sql = stripLineComments(readResource("db/schema.sql"));
            for (String statement : splitStatements(sql)) {
                if (!statement.isBlank()) {
                    st.execute(statement);
                }
            }
        }
    }

    /** So semicolons inside -- comments do not break naive splitting. */
    private static String stripLineComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\\R")) {
            int dash = line.indexOf("--");
            if (dash >= 0) {
                line = line.substring(0, dash);
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String[] splitStatements(String sql) {
        return sql.split(";");
    }
}
