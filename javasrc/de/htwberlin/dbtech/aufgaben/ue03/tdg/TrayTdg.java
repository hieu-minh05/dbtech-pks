package de.htwberlin.dbtech.aufgaben.ue03.tdg;

import java.sql.*;

public class TrayTdg {

    private final Connection connection;

    public TrayTdg(Connection connection) {
        this.connection = connection;
    }

    public Integer findSuitableTray(Integer diameterInCM, Integer sampleId)
            throws SQLException {
        String sql =
                "SELECT t.trayid " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON p.trayid = t.trayid " +
                        "WHERE t.diameterincm = ? " +
                        "AND t.expirationdate > ( " +
                        "   SELECT expirationdate " +
                        "   FROM sample " +
                        "   WHERE sampleid = ? " +
                        ") " +
                        "GROUP BY t.trayid, t.expirationdate, t.capacity " +
                        "HAVING COUNT(p.placeno) < t.capacity " +
                        "ORDER BY t.expirationdate " +
                        "FETCH FIRST 1 ROW ONLY";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, diameterInCM);
            stmt.setInt(2, sampleId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("trayid");
                }
            }
        }

        return null;
    }

    public Integer getCapacity(Integer trayId)
            throws SQLException {
        String sql =
                "SELECT capacity " +
                        "FROM tray " +
                        "WHERE trayid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, trayId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("capacity");
                }
            }
        }

        return null;
    }

    public Integer findEmptyTray(Integer diameterInCM)
            throws SQLException {

        String sql =
                "SELECT t.trayid " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON p.trayid = t.trayid " +
                        "WHERE t.diameterincm = ? " +
                        "GROUP BY t.trayid " +
                        "HAVING COUNT(p.sampleid) = 0 " +
                        "FETCH FIRST 1 ROW ONLY";

        try (PreparedStatement stmt =
                     connection.prepareStatement(sql)) {

            stmt.setInt(1, diameterInCM);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("trayid");
                }
            }
        }

        return null;
    }

    public void updateExpirationDate(Integer trayId,
                                     Integer sampleId)
            throws SQLException {

        String sql =
                "UPDATE tray " +
                        "SET expirationdate = ( " +
                        "   SELECT expirationdate + 30 " +
                        "   FROM sample " +
                        "   WHERE sampleid = ? " +
                        ") " +
                        "WHERE trayid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, sampleId);
            stmt.setInt(2, trayId);

            stmt.executeUpdate();
        }
    }
}