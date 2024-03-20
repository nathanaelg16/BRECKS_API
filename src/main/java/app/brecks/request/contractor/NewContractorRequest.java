package app.brecks.request.contractor;

import app.brecks.request.Request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewContractorRequest implements Request {
    private String entityName;
    private String shortName;

    @Override
    public boolean isWellFormed() {
        if (shortName == null || shortName.isBlank()) shortName = entityName;
        return this.entityName != null && !this.entityName.isBlank();

}
