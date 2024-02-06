package app.brecks.dao.contractor;

import app.brecks.model.contractor.Contractor;

import java.sql.SQLException;
import java.util.List;

public interface IContractorDAO {
    List<Contractor> getContractors() throws SQLException;
}
