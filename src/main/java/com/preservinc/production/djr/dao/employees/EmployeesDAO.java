package com.preservinc.production.djr.dao.employees;

import com.preservinc.production.djr.model.employee.Employee;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class EmployeesDAO implements IEmployeesDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public EmployeesDAO(DataSource dataSource) {
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
}
