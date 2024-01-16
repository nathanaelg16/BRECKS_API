package com.preservinc.production.djr.dao.employees;

import com.preservinc.production.djr.exception.employee.AlreadyAddedException;
import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.request.employee.AddEmployeeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class EmployeeDAO implements IEmployeeDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public EmployeeDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Employee findEmployeeByID(int id) throws SQLException {
        logger.info("[EmployeesDAO] Retrieving employee with ID: " + id);
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT first_name, last_name, display_name, role, email, admin FROM Employees WHERE id = ?;")
        ) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return new Employee(id,
                            r.getString("first_name"),
                            r.getString("last_name"),
                            r.getString("display_name"),
                            r.getString("role"),
                            r.getString("email"),
                            r.getBoolean("admin"));
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public Employee findEmployeeByUsername(String username) throws SQLException {
        logger.info("[EmployeesDAO] Retrieving employee with username: " + username);
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id, first_name, last_name, display_name, role, email, admin FROM Employees WHERE username = ?;")
        ) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return new Employee(r.getInt("id"),
                            r.getString("first_name"),
                            r.getString("last_name"),
                            r.getString("display_name"),
                            r.getString("role"),
                            r.getString("email"),
                            r.getBoolean("admin"));
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public void createEmployee(AddEmployeeRequest request) throws SQLException {
        logger.info("[Employee DAO] Creating employee on database with email: {}", request.getEmail());
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("insert into Employees (first_name, last_name, role, email) value (?, ?, ?, ?);")
        ) {
            p.setString(1, request.getFirstName());
            p.setString(2, request.getLastName());
            p.setString(3, request.getRole());
            p.setString(4, request.getEmail());
            p.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.info("[Employee DAO] Integrity constraint violation occurred: {}", e.getMessage());
            logger.info(e);
            throw new AlreadyAddedException();
        }
    }
}
