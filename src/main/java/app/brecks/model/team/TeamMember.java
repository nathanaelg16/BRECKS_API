package app.brecks.model.team;

import app.brecks.model.Views;
import app.brecks.model.employee.Employee;
import app.brecks.model.employee.EmployeeStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString
@JsonView({Views.Default.class})
public final class TeamMember extends Employee {
    @JsonProperty
    private final @NonNull TeamMemberRole teamRole;

    public TeamMember(int id, String firstName, String lastName, String displayName, String role, String email, Boolean isAdmin, EmployeeStatus status, @NonNull TeamMemberRole teamMemberRole) {
        super(id, firstName, lastName, displayName, role, email, isAdmin, status);
        this.teamRole = teamMemberRole;
    }
}
