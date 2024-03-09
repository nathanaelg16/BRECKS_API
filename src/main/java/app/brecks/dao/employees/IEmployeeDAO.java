package app.brecks.dao.employees;

import app.brecks.model.employee.Employee;
import app.brecks.request.employee.AddEmployeeRequest;

import java.sql.SQLException;

public interface IEmployeeDAO {
    Employee findEmployeeByID(int id) throws SQLException;

    Employee findEmployeeByUsername(String username) throws SQLException;

    Employee findEmployeeByEmail(String email) throws SQLException;

    void createEmployee(AddEmployeeRequest request) throws SQLException;
}
