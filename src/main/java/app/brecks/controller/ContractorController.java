package app.brecks.controller;

import app.brecks.exception.BadRequestException;
import app.brecks.model.contractor.Contractor;
import app.brecks.request.contractor.NewContractorRequest;
import app.brecks.service.contractor.IContractorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contractors")
public class ContractorController {
    private final static Logger logger = LogManager.getLogger();
    private final IContractorService contractorService;

    @Autowired
    public ContractorController(IContractorService contractorService) {
        this.contractorService = contractorService;
    }

    @GetMapping
    public ResponseEntity<List<Contractor>> getContractors() {
        logger.traceEntry("getContractors()");
        return ResponseEntity.ok(this.contractorService.getContractors());
    }

    @PostMapping
    public ResponseEntity<Void> addContractor(NewContractorRequest contractorRequest) {
        logger.traceEntry("addContractor(contractorRequest={})", contractorRequest);
        if (!contractorRequest.isWellFormed()) throw new BadRequestException();
        this.contractorService.addContractor(contractorRequest);
        return ResponseEntity.ok().build();
    }
}
