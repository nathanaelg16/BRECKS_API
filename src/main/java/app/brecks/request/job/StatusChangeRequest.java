package app.brecks.request.job;

import app.brecks.model.job.JobStatus;
import app.brecks.request.Request;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StatusChangeRequest implements Request {
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    public JobStatus getStatus() {
        return  JobStatus.of(status);
    }

    @Override
    public boolean isWellFormed() {
        if (status == null) return false;
        if (startDate == null) return false;

        try {
            JobStatus.of(status);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }
}
