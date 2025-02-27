// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.FileHelper;
// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public abstract class SubjectRecordDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectRecordDB.class.getName());
    // This is the string used to join the subject ID and record date.
    public static String joinStr = "=";
    
    // Insert this subject record into database.    
    public static boolean insertSR(SubjectRecord sr, Connection conn) 
            throws SQLException {
        boolean result = Constants.OK;
        // This operation will fail if the Excel file contains duplicated
        // subject record, hence need to set a savepoint for rollback and
        // to continue with the next record if it happens.
        Savepoint sp = conn.setSavepoint();
        StringBuilder detail = new StringBuilder(sr.getStudy_id()).
                                   append(" - ").append(sr.getSubject_id()).
                                   append(" - ").append(sr.getRecord_date());
//        String detail = sr.getStudy_id() + " - " + sr.getSubject_id() 
//                      + " - " + sr.getRecord_date();
        String query = "INSERT INTO subject_record(subject_id,study_id,"
                     + "record_date,height,weight,dat) VALUES (?,?,?,?,?,?)";
        
        try {
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, sr.getSubject_id());
            stm.setString(2, sr.getStudy_id());
            stm.setObject(3, sr.getRecord_date(), Types.DATE);
            stm.setString(4, sr.getHeight());
            stm.setString(5, sr.getWeight());
            stm.setBytes(6, FileHelper.convertObjectToByteArray(sr.getDataValueList()));
            stm.executeUpdate();
            stm.close();
            // Operation is successful, release the savepoint.
            conn.releaseSavepoint(sp);
            logger.info("Insert subject record: " + detail);
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert subject record: " + detail);
            logger.error(e.getMessage());
            // Operation failed, rollback.
            logger.info("Rolling back.");
            conn.rollback(sp);
        }

        return result;
    }
    
    // Delete all the subject records belonging to this study ID.
    public static void deleteAllSubjectRecordsFromStudy(String study_id) {
        Connection conn = null;
        String query = "DELETE FROM subject_record WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to delete subject records!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // This express get method is used during consistency check in meta data 
    // upload.
    public static SubjectRecord getSubjectRecord(PreparedStatement stm, 
            String study_id, String subject_id, LocalDate record_date) 
            throws SQLException 
    {
        SubjectRecord sr = null;
        stm.setString(1, study_id);
        stm.setString(2, subject_id);
        stm.setObject(3, record_date, Types.DATE);
        ResultSet rs = stm.executeQuery();    
        if (rs.next()) {
            sr = new SubjectRecord(rs);
        }
        return sr;
    }
    
    // Return the subject record belonging to this primary key (i.e. study_id +
    // subject_id + record_date.)
    public static SubjectRecord getSubjectRecord(String study_id, 
            String subject_id, LocalDate record_date) {
        Connection conn = null;
        SubjectRecord sr = null;
        String query = "SELECT * From subject_record WHERE study_id = ? AND "
                     + "subject_id = ? AND record_date = ?";
        
        try {
            conn =DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.setString(2, subject_id);
            stm.setObject(3, record_date, Types.DATE);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                sr = new SubjectRecord(rs);
            }
        }
        catch (SQLException|NamingException e) {
            StringBuilder err = new 
                StringBuilder("FAIL to retrieve subject record: ").
                    append(study_id).append("-").append(subject_id).
                    append("-").append(record_date);
            logger.error(err);
//            logger.error("FAIL to retrieve subject record: " + study_id + 
//                         "-" + subject_id + "-" + record_date);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return sr;
    }
    
    // Return the list of subject_id-record_date belonging to this study_id.
    public static List<String> getSubjectRecordDateList(String study_id) {
        Connection conn = null;
        List<String> srdList = new ArrayList<>();
        String query = "SELECT subject_id, record_date FROM subject_record "
                     + "WHERE study_id = ? ORDER BY subject_id, record_date";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                StringBuilder tmp = new StringBuilder(rs.getString("subject_id")).
                                        append(joinStr).
                                        append(rs.getString("record_date"));
                srdList.add(tmp.toString());
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve list of subject_id-record_date!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return srdList;
    }
    
    /* NOT IN USE!
    // Return the list of subject records belonging to this study.
    public static List<SubjectRecord> getSSList(String study_id) {
        Connection conn = null;
        List<SubjectRecord> srList = new ArrayList<>();
        String query = "SELECT * FROM subject_record WHERE study_id = ? "
                     + "ORDER BY subject_id, record_date";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                SubjectRecord sr = new SubjectRecord(
                                    rs.getString("subject_id"),
                                    rs.getString("study_id"),
                                    rs.getObject("record_date", LocalDate.class),
                                    rs.getString("remarks"),
                                    rs.getString("event"),
                                    rs.getString("height"),
                                    rs.getString("weight"),
                                    rs.getObject("event_date", LocalDate.class),
                                    // For now, initialise as null.
                                    null);
                
                srList.add(sr);
            }
            logger.debug("Subject records retrieved from study: " + study_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve subject records from study: " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return srList;
    }
    */
    
    // Update subject record from this study in database. Called when user 
    // decided to skip consistency check during meta data upload through Excel.
    // Only allow update to height, weight, sample_id and dat.
    public static boolean updatePartialSubjectRecord
                            (SubjectRecord sr, Connection conn) 
    {
        boolean result = Constants.OK;
        StringBuilder detail = new StringBuilder(sr.getStudy_id()).
                                   append(" - ").append(sr.getSubject_id()).
                                   append(" - ").append(sr.getRecord_date());
//        String detail = sr.getStudy_id() + " - " + sr.getSubject_id() 
//                      + " - " + sr.getRecord_date();
        String query = "UPDATE subject_record SET height = ?, weight = ?, "
                     + "sample_id = ?, dat = ? WHERE subject_id = ? "
                     + "AND record_date = ? AND study_id = ?";
        
        try {
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, sr.getHeight());
            stm.setString(2, sr.getWeight());
            stm.setString(3, sr.getSample_id());
            stm.setBytes(4, FileHelper.convertObjectToByteArray(sr.getDataValueList()));            
            stm.setString(5, sr.getSubject_id());
            stm.setObject(6, sr.getRecord_date(), Types.DATE);
            stm.setString(7, sr.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.info("Update subject record: " + detail);
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update subject record: " + detail);
            logger.error(e.getMessage());
        }
        
        return result;        
    }
    
    /* NO LONGER IN USE!
    // Update subject record from this study in database.
    // Allow changes to height, weight and record_date.
    public static boolean updateSR(SubjectRecord sr, LocalDate new_record_date) {
        boolean result = Constants.OK;
        Date rec_date = null;
        Date new_rec_date = null;
        String detail = sr.getStudy_id() + " - " + sr.getSubject_id() 
                      + " - " + new_record_date;
        Connection conn = null;
        String query = "UPDATE subject_record SET height = ?, weight = ?, "
                     + "record_date = ? WHERE subject_id = ? AND "
                     + "record_date = ? AND study_id = ?";
        
        rec_date = java.sql.Date.valueOf(sr.getRecord_date());
        new_rec_date = java.sql.Date.valueOf(new_record_date);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, sr.getHeight());
            stm.setString(2, sr.getWeight());
            stm.setDate(3, new_rec_date);
            stm.setString(4, sr.getSubject_id());
            stm.setDate(5, rec_date);
            stm.setString(6, sr.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Update subject record: " + detail);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update subject record: " + detail);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    */
    
    // Check whether any subject record exists in the database for this subject. 
    // Exception thrown here need to be handle by the caller.
    public static boolean isSRExist(String subject_id, String study_id) 
            throws SQLException, NamingException
    {
        Connection conn = null;
        String query = "SELECT * FROM subject_record WHERE subject_id = ? AND "
                     + "study_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, study_id);
        ResultSet rs = stm.executeQuery();
        boolean srExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return srExist;
    }
    
    // Check whether the specific subject record (with record date) exists in 
    // the database. Exception thrown here need to be handle by the caller.
    public static boolean isSRExist(String subject_id, String study_id, LocalDate rec_date) 
            throws SQLException, NamingException
    {
        Connection conn = null;
        String query = "SELECT * FROM subject_record WHERE subject_id = ? AND "
                     + "study_id = ? AND record_date = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, study_id);
        stm.setObject(3, rec_date, Types.DATE);
        ResultSet rs = stm.executeQuery();
        boolean srExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return srExist;
    }
}
