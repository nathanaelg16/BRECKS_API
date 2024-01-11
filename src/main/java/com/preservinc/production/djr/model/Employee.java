package com.preservinc.production.djr.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record Employee(int id, String uid, String firstName, String lastName, String displayName, String role, String email, Boolean isAdmin) {
    public String fullName() {
        return "%s %s".formatted(this.firstName, this.lastName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Employee employee = (Employee) o;

        return new EqualsBuilder().append(id, employee.id).append(uid, employee.uid).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(uid).toHashCode();
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}