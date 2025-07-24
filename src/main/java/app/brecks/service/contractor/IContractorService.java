package app.brecks.service.contractor;

import app.brecks.model.contractor.Contractor;
import app.brecks.request.contractor.NewContractorRequest;

import java.util.List;

public interface IContractorService {
    List<Contractor> getContractors();

    void addContractor(NewContractorRequest contractorRequest);
}
