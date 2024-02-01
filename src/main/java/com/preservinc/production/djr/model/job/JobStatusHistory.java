package com.preservinc.production.djr.model.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.preservinc.production.djr.model.time.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobStatusHistory {
    private Map<JobStatus, List<Interval>> statusHistory;

    private JobStatusHistory() {
        this.statusHistory = new HashMap<>();
        this.statusHistory.put(JobStatus.NOT_STARTED, new ArrayList<>());
        this.statusHistory.put(JobStatus.ACTIVE, new ArrayList<>());
        this.statusHistory.put(JobStatus.ON_HOLD, new ArrayList<>());
        this.statusHistory.put(JobStatus.COMPLETED, new ArrayList<>());
    }

    private void addInterval(JobStatus status, Interval interval) {
        this.statusHistory.get(status).add(interval);
    }

    @JsonIgnore
    public List<Interval> getIntervalsWith(JobStatus status) {
        return this.statusHistory.get(status);
    }

    @JsonProperty("active")
    public List<Interval> getActiveIntervals() {
        return this.statusHistory.get(JobStatus.ACTIVE);
    }

    @JsonProperty("on_hold")
    public List<Interval> getOnHoldIntervals() {
        return this.statusHistory.get(JobStatus.ON_HOLD);
    }

    @JsonProperty("completed")
    public List<Interval> getCompletedIntervals() {
        return this.statusHistory.get(JobStatus.COMPLETED);
    }

    @JsonProperty("not_started")
    public List<Interval> getNotStartedIntervals() {
        return this.statusHistory.get(JobStatus.NOT_STARTED);
    }

    public static class Builder {
        private final JobStatusHistory jobStatusHistory;

        public Builder() {
            this.jobStatusHistory = new JobStatusHistory();
        }

        public Builder addInterval(JobStatus status, Interval interval) {
            this.jobStatusHistory.addInterval(status, interval);
            return this;
        }

        public Builder addActiveInterval(Interval interval) {
            this.jobStatusHistory.addInterval(JobStatus.ACTIVE, interval);
            return this;
        }

        public Builder addOnHoldInterval(Interval interval) {
            this.jobStatusHistory.addInterval(JobStatus.ON_HOLD, interval);
            return this;
        }

        public Builder addNotStartedInterval(Interval interval) {
            this.jobStatusHistory.addInterval(JobStatus.NOT_STARTED, interval);
            return this;
        }

        public Builder addCompletedInterval(Interval interval) {
            this.jobStatusHistory.addInterval(JobStatus.COMPLETED, interval);
            return this;
        }

        public JobStatusHistory build() {
            return this.jobStatusHistory;
        }
    }
}