package de.htwberlin.dbtech.aufgaben.ue03.tdg;

import java.sql.*;

public class SampleTdg {

    private final Connection connection;

    public SampleTdg(Connection connection) {
        this.connection = connection;
    }

    public Date getExpirationDate(Integer sampleId)
            throws SQLException {

        String sql =
                "SELECT expirationdate " +
                        "FROM sample " +
                        "WHERE sampleid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, sampleId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getDate("expirationdate");
                }
            }
        }

        return null;
    }
}