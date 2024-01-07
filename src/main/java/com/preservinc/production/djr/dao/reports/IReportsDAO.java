package com.preservinc.production.djr.dao.reports;

import com.preservinc.production.djr.model.Report;

import java.sql.SQLException;
import java.util.List;

public interface IReportsDAO {
    void saveReport(Report report) throws SQLException;
    List<String> getEmailsForReportAdmins() throws SQLException;
}
