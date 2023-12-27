package com.preservinc.production.djr.dao.reports;

import com.preservinc.production.djr.model.Report;

import java.sql.SQLException;

public interface IReportsDAO {
    void saveReport(Report report) throws SQLException;
}
