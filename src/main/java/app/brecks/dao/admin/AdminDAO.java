package app.brecks.dao.admin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class AdminDAO implements IAdminDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public AdminDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
