package app.brecks.model.employee;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class Employee {
    private final int id;
    private final String firstName;
    private final String lastName;
    private final String displayName;
    private final String role;
    private final String email;
    private final Boolean isAdmin;
    private final EmployeeStatus status;

    @BsonCreator
    public Employee(@BsonProperty("id") int id, @BsonProperty("firstName") String firstName, @BsonProperty("lastName") String lastName, @BsonProperty("displayName") String displayName, @BsonProperty("role") String role, @BsonProperty("email") String email, @BsonProperty("isAdmin") Boolean isAdmin, @BsonProperty("status") EmployeeStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.role = role;
        this.email = email;
        this.isAdmin = isAdmin;
        this.status = status;
    }

    @JsonProperty
    public String fullName() {
        return "%s %s".formatted(this.firstName, this.lastName);
    }

    @JsonProperty
    public int id() {
        return id;
    }

    @JsonProperty
    public String firstName() {
        return firstName;
    }

    @JsonProperty
    public String lastName() {
        return lastName;
    }

    @JsonProperty
    public String displayName() {
        return displayName;
    }

    @JsonProperty
    public String role() {
        return role;
    }

    @JsonProperty
    public String email() {
        return email;
    }

    @JsonProperty
    public Boolean isAdmin() {
        return isAdmin;
    }

    @JsonProperty
    public EmployeeStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Employee employee = (Employee) o;

        return new EqualsBuilder().append(id, employee.id).append(firstName, employee.firstName).append(lastName, employee.lastName).append(displayName, employee.displayName).append(role, employee.role).append(email, employee.email).append(isAdmin, employee.isAdmin).append(status, employee.status).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(firstName).append(lastName).append(displayName).append(role).append(email).append(isAdmin).append(status).toHashCode();
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}