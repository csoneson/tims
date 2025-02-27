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
// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class DashboardConfigDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DashboardConfigDB.class.getName());
    private final String study_id;
    // Machine generated constructor
    public DashboardConfigDB(String study_id) {
        this.study_id = study_id;
    }

    // Return the list of dashboard config that belong to this study.
    public List<DashboardConfig> getDashboardConfigList() {
        Connection conn = null;
        List<DashboardConfig> dbConfigList = new ArrayList<>();
        String query = "SELECT * FROM dashboard_config WHERE study_id = ? "
                     + "ORDER BY chart_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                dbConfigList.add(new DashboardConfig(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve the dashboard config for " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return dbConfigList;
    }
    
    // Return the dashboard config for this chart ID defined under this study.
    public DashboardConfig getDBCForChartID(String chart_id) {
        DashboardConfig dbc = null;
        Connection conn = null;
        String query = "SELECT * FROM dashboard_config WHERE chart_id = ? AND "
                     + "study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, chart_id);
            stm.setString(2, study_id);
            ResultSet rs = stm.executeQuery();

            if (rs.next()) {
                dbc = new DashboardConfig(rs);
            }
            stm.close();            
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve the dashboard config for " + chart_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return dbc;
    }
    
    // Insert the new dashboard configuration into database.
    public void insertDBCsForNewStudy() {
        Connection conn = null;
        String query = "INSERT INTO dashboard_config(chart_id,data_source_x,data_source_y,title,study_id) "
                     + "VALUES(\'PIECL\',\'age\',\'\',\'Age Group Breakdown Chart\',\'" + study_id 
                     + "\'),(\'PIECR\',\'race\',\'\',\'Ethnicity Group Breakdown Chart\',\'" + study_id
                     + "\'),(\'BARCL\',\'race\',\'gender\',\'Ethnicity Breakdown by Gender Chart\',\'" + study_id
                     + "\'),(\'BARCR\',\'race\',\'casecontrol\',\'Ethnicity Breakdown by Case Control Chart\',\'" + study_id + "\')";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to create dashboard config for " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Update title, data_source_x, data_source_y and inverted in database.
    public boolean updateDBC(DashboardConfig dbc) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE dashboard_config SET title = ?, "
                     + "data_source_x = ?, data_source_y = ?, label_x = ? "
                     + "WHERE chart_id = ? AND study_id = ?";
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dbc.getTitle());
            stm.setString(2, dbc.getData_source_x());
            stm.setString(3, dbc.getData_source_y());
            stm.setString(4, dbc.getLabel_x());
            stm.setString(5, dbc.getChart_id());
            stm.setString(6, dbc.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.info("Updated dashboard config for " + dbc.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update dashboard config for " + dbc.getStudy_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
}
