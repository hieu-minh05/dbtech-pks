package de.htwberlin.dbtech.aufgaben.ue03.tdg;

import java.sql.*;

public class PlaceTdg {

    private final Connection connection;

    public PlaceTdg(Connection connection) {
        this.connection = connection;
    }

    public boolean isOccupied(Integer trayId,
                              Integer placeNo)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) " +
                        "FROM place " +
                        "WHERE trayid = ? " +
                        "AND placeno = ?";

        try (PreparedStatement stmt =
                     connection.prepareStatement(sql)) {

            stmt.setInt(1, trayId);
            stmt.setInt(2, placeNo);

            try (ResultSet rs = stmt.executeQuery()) {

                rs.next();

                return rs.getInt(1) > 0;
            }
        }
    }

    public void insert(Integer trayId,
                       Integer placeNo,
                       Integer sampleId)
            throws SQLException {

        String sql =
                "INSERT INTO place " +
                        "(trayid, placeno, sampleid) " +
                        "VALUES (?, ?, ?)";

        try (PreparedStatement stmt =
                     connection.prepareStatement(sql)) {

            stmt.setInt(1, trayId);
            stmt.setInt(2, placeNo);
            stmt.setInt(3, sampleId);

            stmt.executeUpdate();
        }
    }
}