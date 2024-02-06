package app.brecks.controller;

import app.brecks.model.contractor.Contractor;
import app.brecks.service.contractor.IContractorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
        logger.info("[Contractor Controller] Received request for contractors...");
        return ResponseEntity.ok(this.contractorService.getContractors());
    }
}
