package com.preservinc.production.djr.request.employee;

import com.preservinc.production.djr.request.Request;
import lombok.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddEmployeeRequest implements Request {
    private String firstName;
    private String lastName;
    private String role;
    private String email;

    @Getter(AccessLevel.NONE)
    private Boolean admin = null;

    public Boolean isAdmin() {
        return this.admin;
    }

    @Override
    public boolean isWellFormed() {
        return BooleanUtils.and(new boolean[]{
                firstName != null && !firstName.isBlank(),
                lastName != null && !lastName.isBlank(),
                role != null && !role.isBlank(),
                email != null && !email.isBlank()
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AddEmployeeRequest that = (AddEmployeeRequest) o;

        return new EqualsBuilder().append(getFirstName(), that.getFirstName()).append(getLastName(), that.getLastName()).append(getRole(), that.getRole()).append(getEmail(), that.getEmail()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getFirstName()).append(getLastName()).append(getRole()).append(getEmail()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("firstName", firstName)
                .append("lastName", lastName)
                .append("role", role)
                .append("email", email)
                .toString();
    }
}
