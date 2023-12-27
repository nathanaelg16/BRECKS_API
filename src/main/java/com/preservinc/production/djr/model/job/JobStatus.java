package com.preservinc.production.djr.model.job;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum JobStatus {
    NOT_STARTED ("NOT STARTED"),
    ACTIVE ("ACTIVE"),
    ON_HOLD ("ON HOLD"),
    COMPLETED ("COMPLETED");

    private static final Map<String, JobStatus> map = new HashMap<>(values().length, 1);

    static {
        for (JobStatus jobStatus : values()) map.put(jobStatus.status, jobStatus);
    }

    private final String status;

    JobStatus(String status) {
        this.status = status;
    }

    public static JobStatus of(String status) {
        JobStatus jobStatus = map.get(status);

        if (jobStatus == null) {
            throw new IllegalArgumentException("Invalid job status: " + status);
        }

        return jobStatus;
    }
}
