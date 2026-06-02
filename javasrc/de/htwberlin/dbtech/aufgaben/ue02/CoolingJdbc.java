package de.htwberlin.dbtech.aufgaben.ue02;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.htwberlin.dbtech.exceptions.CoolingSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.htwberlin.dbtech.exceptions.DataException;

import javax.xml.transform.Result;


public class CoolingJdbc implements ICoolingJdbc {

    private static final Logger L = LoggerFactory.getLogger(CoolingJdbc.class);
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
    public List<String> getSampleKinds() {
        L.info("getSampleKinds: start");
        List<String> result = new ArrayList<>();
        String sql = "SELECT text FROM samplekind";
        try (PreparedStatement s = connection.prepareStatement(sql);
             ResultSet rs = s.executeQuery()
        ) {
            while (rs.next()) {
                result.add(rs.getString("text"));

            }

        } catch (SQLException e) {
            throw new CoolingSystemException("Error in getSampleKinds", e);
        }
        return result;

    }

    @Override
    public Sample findSampleById(Integer sampleId) {
        L.info("findSampleById: sampleId: " + sampleId);

        String sql = "select * from sample where sampleid=?";
        try (PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setInt(1, sampleId);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) {
                    throw new CoolingSystemException("Sample not found");
                }
                Sample sample = new Sample();
                sample.setSampleId(rs.getInt("sampleid"));
                sample.setSampleKindId(rs.getInt("samplekindid"));
                sample.setExpirationDate(rs.getDate("expirationdate").toLocalDate());

                return sample;
            }
        } catch (SQLException e) {
            throw new CoolingSystemException("Error in findSampleById", e);
        }


    }

    @Override
    public void createSample(Integer sampleId, Integer sampleKindId) {
        L.info("createSample: sampleId: " + sampleId + ", sampleKindId: " + sampleKindId);
        String checkSample = "select count(*) as anzahl_sample from sample where sampleid=?";
        String checkKind = "select count(*) as anzahl_kind from samplekind where samplekindid=?";
        String insertSql = "insert into sample (sampleid, samplekindid, expirationdate) values (?,?,?)";
        try {
            //check if sample exists
            try (PreparedStatement checkSampleStmt = connection.prepareStatement(checkSample)) {
                checkSampleStmt.setInt(1, sampleId);
                try (ResultSet rs1 = checkSampleStmt.executeQuery()) {
                    if (!rs1.next() || rs1.getInt("anzahl_sample") == 0) {
                        throw new CoolingSystemException(sampleId + "Sample not found");
                    }
                }

            }

            //check if sample kind exists
            try (PreparedStatement checkKindStmt = connection.prepareStatement(checkKind)) {
                checkKindStmt.setInt(1, sampleId);
                try (ResultSet rs2 = checkKindStmt.executeQuery()) {
                    if (!rs2.next() || rs2.getInt("anzahl_kind") == 0) {
                        throw new CoolingSystemException(sampleKindId + "Sample kind not found");
                    }
                }
            }
            int validDays;
            switch (sampleKindId) {
                case 1:
                    validDays = 4;
                    break;   // Blood
                case 2:
                    validDays = 5;
                    break;   // Serum
                case 3:
                    validDays = 6;
                    break;   // Urine
                default:
                    validDays = 0;
            }
            LocalDate expirationDate = LocalDate.now().plusDays(validDays);

            //insert new sample
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setInt(1, sampleId);
                insertStmt.setInt(2, sampleKindId);
                insertStmt.setDate(3, java.sql.Date.valueOf(expirationDate));

                insertStmt.executeUpdate();
            } catch (SQLException e) {
                throw new CoolingSystemException("Error in createSample", e);
            }
        } catch (SQLException e) {
            throw new CoolingSystemException("Error in createSample", e);
        }
    }


        @Override
        public void clearTray(Integer trayId){
            L.info("clearTray: trayId: " + trayId);

            String deleteSql = "delete from sample where sampleid=?";
            String checkTraySQL = "select count(*) as anzahl_tray from tray where trayid=?";
            try(PreparedStatement checkTray = connection.prepareStatement(checkTraySQL)){
                checkTray.setInt(1, trayId);

                try(ResultSet rs = checkTray.executeQuery()){
                    if(!rs.next() || rs.getInt("anzahl_tray") == 0){
                        throw new CoolingSystemException("Tray does not exist: " + trayId);
                    }
                }
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)){
                    deleteStmt.setInt(1, trayId);
                    deleteStmt.executeUpdate();
                }
            } catch(SQLException e){
                throw new CoolingSystemException("Error in clearTray", e);
            }
        }
}

