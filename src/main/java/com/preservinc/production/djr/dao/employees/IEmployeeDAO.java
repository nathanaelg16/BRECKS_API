package com.preservinc.production.djr.dao.employees;

import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.request.employee.AddEmployeeRequest;

import java.sql.SQLException;

public interface IEmployeeDAO {
    Employee findEmployeeByID(int id) throws SQLException;

    Employee findEmployeeByUsername(String username) throws SQLException;

    void createEmployee(AddEmployeeRequest request) throws SQLException;
}
