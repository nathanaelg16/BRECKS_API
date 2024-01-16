package com.preservinc.production.djr.dao.employees;

import com.preservinc.production.djr.model.employee.Employee;

import java.sql.SQLException;

public interface IEmployeesDAO {
    Employee findEmployeeByID(int id) throws SQLException;

    Employee findEmployeeByUsername(String tokenUser);
}
