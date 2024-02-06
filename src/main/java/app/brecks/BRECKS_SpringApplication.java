package app.brecks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BRECKS_SpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BRECKS_SpringApplication.class, args);
    }

}

// todo post initialization, create root user with default password if not exists