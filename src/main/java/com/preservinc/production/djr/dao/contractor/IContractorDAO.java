package com.preservinc.production.djr.dao.contractor;

import com.preservinc.production.djr.model.contractor.Contractor;

import java.sql.SQLException;
import java.util.List;

public interface IContractorDAO {
    List<Contractor> getContractors() throws SQLException;
}
