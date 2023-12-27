package com.preservinc.production.djr.dao.reports;

import com.preservinc.production.djr.model.Report;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class ReportsDAO implements IReportsDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public ReportsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveReport(Report report) throws SQLException {
        logger.info("[ReportsDAO] Saving report to database.\nJob ID: {}\tReport Date: {}\tBy: ID #{}", report.getJobID(), report.getReportDate(), report.getReportBy());
    }
}
