package app.brecks.service.contractor;

import app.brecks.dao.contractor.IContractorDAO;
import app.brecks.exception.DatabaseException;
import app.brecks.exception.contractor.DuplicateContractorException;
import app.brecks.model.contractor.Contractor;
import app.brecks.request.contractor.NewContractorRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class ContractorService implements IContractorService {
    private final Logger logger = LogManager.getLogger();
    private final IContractorDAO contractorDAO;

    @Autowired
    public ContractorService(IContractorDAO contractorDAO) {
        this.contractorDAO = contractorDAO;
    }

    @Override
    public List<Contractor> getContractors() {
        this.logger.traceEntry("getContractors()");
        try {
            return this.contractorDAO.getContractors();
        } catch (SQLException e) {
            throw new DatabaseException();
        }
    }

    @Override
    public void addContractor(NewContractorRequest contractorRequest) {
        this.logger.traceEntry("addContractor(contractorRequest={})", contractorRequest);
        try {
            this.contractorDAO.addContractor(contractorRequest);
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) throw new DuplicateContractorException();
            else throw new DatabaseException(e);
        }
    }
}
