package app.brecks.dao.contractor;

import app.brecks.model.contractor.Contractor;
import app.brecks.request.contractor.NewContractorRequest;

import java.sql.SQLException;
import java.util.List;

public interface IContractorDAO {
    List<Contractor> getContractors() throws SQLException;

    void addContractor(NewContractorRequest contractorRequest) throws SQLException;
}
