package com.preservinc.production.djr.model.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.preservinc.production.djr.model.employee.Employee;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Report {
    private int jobID;
    private LocalDate reportDate;
    private String weather;
    private int crewSize;
    private String visitors;
    private String workArea1;
    private String workArea2;
    private String workArea3;
    private String workArea4;
    private String workArea5;
    private String materials1;
    private String materials2;
    private String materials3;
    private String materials4;
    private String subs;
    private boolean onsite;
    private Employee reportBy;
    private Employee PM;
    private Employee PS;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Report report = (Report) o;

        return new EqualsBuilder().append(getJobID(), report.getJobID()).append(getCrewSize(), report.getCrewSize()).append(isOnsite(), report.isOnsite()).append(getReportDate(), report.getReportDate()).append(getWeather(), report.getWeather()).append(getVisitors(), report.getVisitors()).append(getWorkArea1(), report.getWorkArea1()).append(getWorkArea2(), report.getWorkArea2()).append(getWorkArea3(), report.getWorkArea3()).append(getWorkArea4(), report.getWorkArea4()).append(getWorkArea5(), report.getWorkArea5()).append(getMaterials1(), report.getMaterials1()).append(getMaterials2(), report.getMaterials2()).append(getMaterials3(), report.getMaterials3()).append(getMaterials4(), report.getMaterials4()).append(getSubs(), report.getSubs()).append(getReportBy(), report.getReportBy()).append(getPM(), report.getPM()).append(getPS(), report.getPS()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getJobID()).append(getReportDate()).append(getWeather()).append(getCrewSize()).append(getVisitors()).append(getWorkArea1()).append(getWorkArea2()).append(getWorkArea3()).append(getWorkArea4()).append(getWorkArea5()).append(getMaterials1()).append(getMaterials2()).append(getMaterials3()).append(getMaterials4()).append(getSubs()).append(isOnsite()).append(getReportBy()).append(getPM()).append(getPS()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("jobID", jobID)
                .append("reportDate", reportDate)
                .append("weather", weather)
                .append("crewSize", crewSize)
                .append("visitors", visitors)
                .append("workArea1", workArea1)
                .append("workArea2", workArea2)
                .append("workArea3", workArea3)
                .append("workArea4", workArea4)
                .append("workArea5", workArea5)
                .append("materials1", materials1)
                .append("materials2", materials2)
                .append("materials3", materials3)
                .append("materials4", materials4)
                .append("subs", subs)
                .append("onsite", onsite)
                .append("reportBy", reportBy)
                .append("pm", PM)
                .append("ps", PS)
                .toString();
    }
}