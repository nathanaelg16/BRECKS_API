package com.preservinc.production.djr.service.employee;

import com.preservinc.production.djr.dao.employees.IEmployeeDAO;
import com.preservinc.production.djr.exception.DatabaseException;
import com.preservinc.production.djr.exception.email.InvalidEmailAddressException;
import com.preservinc.production.djr.request.employee.AddEmployeeRequest;
import com.preservinc.production.djr.service.email.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;

@Service
public class EmployeeService implements IEmployeeService {
    private static final Logger logger = LogManager.getLogger();
    private final IEmployeeDAO employeeDAO;
    private final IEmailService emailService;

    @Autowired
    public EmployeeService(IEmployeeDAO employeeDAO, IEmailService emailService) {
        this.employeeDAO = employeeDAO;
        this.emailService = emailService;
    }

    @Override
    public void addEmployee(AddEmployeeRequest request) {
        logger.info("[Employee Service] Adding new employee with email: {}", request.getEmail());
        try {
            InternetAddress.parse(request.getEmail());
            this.employeeDAO.createEmployee(request);
            this.emailService.notifyAccountCreation(request.getEmail());
        } catch (AddressException e) {
            logger.info("[Employee Service] Unable to parse email address: {}\nError message: {}", request.getEmail(), e.getMessage());
            logger.info(e);
            throw new InvalidEmailAddressException();
        } catch (SQLException e) {
            logger.error("An SQL exception occurred: {}", e.getMessage());
            logger.error(e);
            throw new DatabaseException();
        } catch (IOException | MessagingException e) {
            logger.error(e);
            logger.error(e.getMessage());
            this.emailService.notifySysAdmin(e);
        }
    }
}
