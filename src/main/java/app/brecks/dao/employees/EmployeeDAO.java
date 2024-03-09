package app.brecks.dao.employees;

import app.brecks.exception.employee.AlreadyAddedException;
import app.brecks.model.employee.Employee;
import app.brecks.model.employee.EmployeeStatus;
import app.brecks.request.employee.AddEmployeeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
             PreparedStatement p = c.prepareStatement("SELECT first_name, last_name, display_name, " +
                     "role, email, admin, status FROM Employees WHERE id = ?;")
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
                            r.getBoolean("admin"),
                            EmployeeStatus.valueOf(r.getString("status")));
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
             PreparedStatement p = c.prepareStatement("SELECT E.id, E.first_name, E.last_name, E.display_name, " +
                     "E.role, E.email, E.admin, E.status FROM Employees E INNER JOIN BRECKS_DEV.EmployeeUserAccounts EUA " +
                     "on E.id = EUA.id WHERE EUA.username = ?;")
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
                            r.getBoolean("admin"),
                            EmployeeStatus.valueOf(r.getString("status")));
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public Employee findEmployeeByEmail(String email) throws SQLException {
        logger.info("[EmployeesDAO] Retrieving employee with email: " + email);
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id, first_name, last_name, display_name, role, " +
                     "email, admin, status FROM Employees WHERE email = ?;")
        ) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return new Employee(r.getInt("id"),
                            r.getString("first_name"),
                            r.getString("last_name"),
                            r.getString("display_name"),
                            r.getString("role"),
                            r.getString("email"),
                            r.getBoolean("admin"),
                            EmployeeStatus.valueOf(r.getString("status")));
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
             PreparedStatement p = c.prepareStatement("insert into Employees (first_name, last_name, " +
                     "display_name, role, email, admin) value (?, ?, ?, ?, ?, ?);")
        ) {
            p.setString(1, request.getFirstName());
            p.setString(2, request.getLastName());
            p.setString(3, request.getFirstName());
            p.setString(4, request.getRole());
            p.setString(5, request.getEmail());
            p.setBoolean(6, request.isAdmin() != null && request.isAdmin());
            p.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.info("[Employee DAO] Integrity constraint violation occurred: {}", e.getMessage());
            logger.info(e);
            throw new AlreadyAddedException();
        }
    }

    public List<Employee> listActiveEmployees() throws SQLException {
        logger.info("[Employee DAO] Listing active employees...");
        return listEmployees(EmployeeStatus.ACTIVE);
    }

    public List<Employee> listInactiveEmployees() throws SQLException {
        logger.info("[Employee DAO] Listing inactive employees...");
        return listEmployees(EmployeeStatus.INACTIVE);
    }

    public List<Employee> listEmployees(EmployeeStatus status) throws SQLException {
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select id, first_name, last_name, display_name, " +
                     "role, email, admin from Employees where status = ?;")
        ) {
            p.setString(1, status.name());
            try (ResultSet r = p.executeQuery()) {
                List<Employee> employees = new ArrayList<>();
                while (r.next()) employees.add(new Employee(r.getInt("id"),
                        r.getString("first_name"),
                        r.getString("last_name"),
                        r.getString("display_name"),
                        r.getString("role"),
                        r.getString("email"),
                        r.getBoolean("admin"),
                        status));
                return employees;
            }
        }
    }
}
