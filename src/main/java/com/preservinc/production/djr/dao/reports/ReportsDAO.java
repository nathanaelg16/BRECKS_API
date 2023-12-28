package com.preservinc.production.djr.dao.reports;

import com.preservinc.production.djr.model.Report;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class ReportsDAO implements IReportsDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public ReportsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveReport(Report report) throws SQLException {
        logger.info("[ReportsDAO] Saving report to database.\nJob ID: {}\tReport Date: {}\tBy: ID #{}",
                report.getJobID(), report.getReportDate(), report.getReportBy());

        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO Reports (job_id, reportDate, weather, " +
                     "crewSize, visitors, workArea1, workArea2, workArea3, workArea4, workArea5, materials1, " +
                     "materials2, materials3, materials4, subs, onsite, report_by) " +
                     "VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        ) {
            p.setInt(1, report.getJobID());
            p.setDate(2, Date.valueOf(report.getReportDate()));
            p.setString(3, report.getWeather());
            p.setInt(4, report.getCrewSize());
            p.setString(5, report.getVisitors());
            p.setString(6, report.getWorkArea1());
            p.setString(7, report.getWorkArea2());
            p.setString(8, report.getWorkArea3());
            p.setString(9, report.getWorkArea4());
            p.setString(10, report.getWorkArea5());
            p.setString(11, report.getMaterials1());
            p.setString(12, report.getMaterials2());
            p.setString(13, report.getMaterials3());
            p.setString(14, report.getMaterials4());
            p.setString(15, report.getSubs());
            p.setBoolean(16, report.isOnsite());
            p.setInt(17, report.getReportBy().id());
            p.executeUpdate();
        }
        logger.info("[ReportsDAO] Successfully saved report to database.");
    }
}
