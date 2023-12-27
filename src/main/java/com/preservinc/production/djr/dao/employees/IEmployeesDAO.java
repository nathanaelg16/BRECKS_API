package com.preservinc.production.djr.dao.employees;

import com.preservinc.production.djr.model.Employee;

import java.sql.SQLException;

public interface IEmployeesDAO {

    Employee findEmployeeByUID(String uid) throws SQLException;

    Employee findEmployeeByID(int id) throws SQLException;
}
