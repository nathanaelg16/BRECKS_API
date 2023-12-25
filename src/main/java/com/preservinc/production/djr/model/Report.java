package com.preservinc.production.djr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.Objects;

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

    public int jobID() {
        return jobID;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public String weather() {
        return weather;
    }

    public int crewSize() {
        return crewSize;
    }

    public String visitors() {
        return visitors;
    }

    public String workArea1() {
        return workArea1;
    }

    public String workArea2() {
        return workArea2;
    }

    public String workArea3() {
        return workArea3;
    }

    public String workArea4() {
        return workArea4;
    }

    public String workArea5() {
        return workArea5;
    }

    public String materials1() {
        return materials1;
    }

    public String materials2() {
        return materials2;
    }

    public String materials3() {
        return materials3;
    }

    public String materials4() {
        return materials4;
    }

    public String subs() {
        return subs;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public boolean onsite() {
        return onsite;
    }

    public void setJobID(int jobID) {
        this.jobID = jobID;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public void setCrewSize(int crewSize) {
        this.crewSize = crewSize;
    }

    public void setVisitors(String visitors) {
        this.visitors = visitors;
    }

    public void setWorkArea1(String workArea1) {
        this.workArea1 = workArea1;
    }

    public void setWorkArea2(String workArea2) {
        this.workArea2 = workArea2;
    }

    public void setWorkArea3(String workArea3) {
        this.workArea3 = workArea3;
    }

    public void setWorkArea4(String workArea4) {
        this.workArea4 = workArea4;
    }

    public void setWorkArea5(String workArea5) {
        this.workArea5 = workArea5;
    }

    public void setMaterials1(String materials1) {
        this.materials1 = materials1;
    }

    public void setMaterials2(String materials2) {
        this.materials2 = materials2;
    }

    public void setMaterials3(String materials3) {
        this.materials3 = materials3;
    }

    public void setMaterials4(String materials4) {
        this.materials4 = materials4;
    }

    public void setSubs(String subs) {
        this.subs = subs;
    }

    public void setOnsite(boolean onsite) {
        this.onsite = onsite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return jobID == report.jobID && crewSize == report.crewSize && onsite == report.onsite && Objects.equals(reportDate, report.reportDate) && Objects.equals(weather, report.weather) && Objects.equals(visitors, report.visitors) && Objects.equals(workArea1, report.workArea1) && Objects.equals(workArea2, report.workArea2) && Objects.equals(workArea3, report.workArea3) && Objects.equals(workArea4, report.workArea4) && Objects.equals(workArea5, report.workArea5) && Objects.equals(materials1, report.materials1) && Objects.equals(materials2, report.materials2) && Objects.equals(materials3, report.materials3) && Objects.equals(materials4, report.materials4) && Objects.equals(subs, report.subs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID, reportDate, weather, crewSize, visitors, workArea1, workArea2, workArea3, workArea4, workArea5, materials1, materials2, materials3, materials4, subs, onsite);
    }

    @Override
    public String toString() {
        return "Report{jobID=%d, reportDate=%s, weather='%s', crewSize=%d, visitors='%s', workArea1='%s', workArea2='%s', workArea3='%s', workArea4='%s', workArea5='%s', materials1='%s', materials2='%s', materials3='%s', materials4='%s', subs='%s', onsite=%s}".formatted(jobID, reportDate, weather, crewSize, visitors, workArea1, workArea2, workArea3, workArea4, workArea5, materials1, materials2, materials3, materials4, subs, onsite);
    }
}