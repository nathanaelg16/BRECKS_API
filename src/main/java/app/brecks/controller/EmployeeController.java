package app.brecks.controller;

import app.brecks.request.employee.AddEmployeeRequest;
import app.brecks.service.employee.IEmployeeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private static final Logger logger = LogManager.getLogger();
    private final IEmployeeService employeeService;

    @Autowired
    public EmployeeController(IEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/add")
    public ResponseEntity<Object> addEmployee(@RequestBody AddEmployeeRequest request) {
        logger.info("[Employee Controller] Received request to add new employee: {}", request);
        if (!request.isWellFormed()) return ResponseEntity.badRequest().body("All fields are required.");
        this.employeeService.addEmployee(request);
        return ResponseEntity.ok().build();
    }
}

// todo implement activate / deactivate employee endpoints
