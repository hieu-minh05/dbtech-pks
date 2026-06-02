package de.htwberlin.dbtech.aufgaben.ue03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.htwberlin.dbtech.exceptions.DataException;

public class CoolingService implements ICoolingService {
    private static final Logger L = LoggerFactory.getLogger(CoolingService.class);
    private Connection connection;

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @SuppressWarnings("unused")
    private Connection useConnection() {
        if (connection == null) {
            throw new DataException("Connection not set");
        }
        return connection;
    }

    @Override
    public void transferSample(Integer sampleId, Integer diameterInCM) {
        L.info("transferSample: sampleId: {}, diameterInCM: {}", sampleId, diameterInCM);
        useConnection();
        String findTray =
                "SELECT t.trayid, t.capacity " +
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
        String checkPlace =
                "SELECT COUNT(*) " +
                        "FROM place " +
                        "WHERE trayid = ? AND placeno = ?";
        String insertPlace =
                "INSERT INTO place(trayid, placeno, sampleid) " +
                        "VALUES (?, ?, ?)";
        String findEmptyTray =
                "SELECT t.trayid " +
                        "FROM tray t " +
                        "LEFT JOIN place p ON p.trayid = t.trayid " +
                        "WHERE t.diameterincm = ? " +
                        "GROUP BY t.trayid " +
                        "HAVING COUNT(p.trayid) = 0 " +
                        "FETCH FIRST 1 ROW ONLY";

        String updateExpiration =
                "UPDATE tray " +
                        "SET expirationdate = ( " +
                        "   SELECT expirationdate + 30 " +
                        "   FROM sample " +
                        "   WHERE sampleid = ? " +
                        ") " +
                        "WHERE trayid = ?";
        try {
            Integer trayId = null;
            Integer capacity = null;

            //1. Passendes Tray suchen
            try (PreparedStatement stmt = connection.prepareStatement(findTray)) {
                stmt.setInt(1, diameterInCM);
                stmt.setInt(2, sampleId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        trayId = rs.getInt("trayid");
                        capacity = rs.getInt("capacity");
                    }
                }
            }

            //2.Passendes Tray gefunden
            if (trayId != null) {
                Integer freePlace = null;
                for (int i = 1; i <= capacity; i++) {
                    try (PreparedStatement stmt = connection.prepareStatement(checkPlace)) {
                        stmt.setInt(1, trayId);
                        stmt.setInt(2, i);

                        try (ResultSet rs = stmt.executeQuery()) {
                            rs.next();
                            if (rs.getInt(1) == 0) {
                                freePlace = i;
                                break;
                            }
                        }
                    }
                }

                if (freePlace == null) {
                    throw new CoolingSystemException("Kein freier Platz gefunden.");
                }

                try (PreparedStatement stmt = connection.prepareStatement(insertPlace)) {

                    stmt.setInt(1, trayId);
                    stmt.setInt(2, freePlace);
                    stmt.setInt(3, sampleId);

                    stmt.executeUpdate();
                }
                return;
            }

            //3. Leeres Tray suchen
            try (PreparedStatement stmt = connection.prepareStatement(findEmptyTray)) {

                stmt.setInt(1, diameterInCM);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        trayId = rs.getInt("trayid");
                    }
                }
            }

            //4. Leeres Tray gefunden
            if (trayId != null) {
                try (PreparedStatement stmt = connection.prepareStatement(updateExpiration)) {
                    stmt.setInt(1, sampleId);
                    stmt.setInt(2, trayId);

                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(insertPlace)) {

                    stmt.setInt(1, trayId);
                    stmt.setInt(2, 1);
                    stmt.setInt(3, sampleId);

                    stmt.executeUpdate();
                }
                return;
            }

            // 5. Nichts gefunden
            throw new CoolingSystemException("Kein passendes Tablett verfügbar.");

        } catch (Exception e) {
            throw new CoolingSystemException("Kein passendes Tablett verfügbar.");
            }
        }
    }



