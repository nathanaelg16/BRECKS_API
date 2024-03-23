package app.brecks.dao.contractor;

import app.brecks.model.contractor.Contractor;
import app.brecks.request.contractor.NewContractorRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ContractorDAO implements IContractorDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    public ContractorDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Contractor> getContractors() throws SQLException {
        logger.traceEntry("getContractors()");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select * from Contractors;");
             ResultSet r = p.executeQuery()
        ) {
            List<Contractor> contractors = new ArrayList<>();
            while (r.next())
                contractors.add(new Contractor(r.getInt("id"), r.getString("entityName"), r.getString("shortName")));
            return contractors;
        }
    }

    @Override
    public void addContractor(NewContractorRequest contractorRequest) throws SQLException {
        logger.traceEntry("addContractor(contractorRequest={})", contractorRequest);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("insert into Contractors (entityName, shortName) value (?, ?);")
        ) {
            p.setString(1, contractorRequest.getEntityName());
            p.setString(2, contractorRequest.getShortName());
            p.executeUpdate();
        }
    }
}
