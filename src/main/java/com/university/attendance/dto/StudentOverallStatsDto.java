package com.university.attendance.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentOverallStatsDto {
    private long totalConducted;
    private long presentCount;
    private long absentCount;
    private double overallPercentage;
    private String statusBadge;
    private List<SubjectAttendanceStatsDto> subjectStats = new ArrayList<>();

    public StudentOverallStatsDto() {
    }

    public StudentOverallStatsDto(long totalConducted, long presentCount, List<SubjectAttendanceStatsDto> subjectStats) {
        this.totalConducted = totalConducted;
        this.presentCount = presentCount;
        this.absentCount = Math.max(0, totalConducted - presentCount);
        if (totalConducted > 0) {
            this.overallPercentage = Math.round((presentCount * 100.0 / totalConducted) * 10.0) / 10.0;
        } else {
            this.overallPercentage = 100.0;
        }
        if (this.overallPercentage >= 80.0) {
            this.statusBadge = "success";
        } else if (this.overallPercentage >= 75.0) {
            this.statusBadge = "warning";
        } else {
            this.statusBadge = "danger";
        }
        this.subjectStats = subjectStats;
    }

    public long getTotalConducted() {
        return totalConducted;
    }

    public void setTotalConducted(long totalConducted) {
        this.totalConducted = totalConducted;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(long absentCount) {
        this.absentCount = absentCount;
    }

    public double getOverallPercentage() {
        return overallPercentage;
    }

    public void setOverallPercentage(double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }

    public String getStatusBadge() {
        return statusBadge;
    }

    public void setStatusBadge(String statusBadge) {
        this.statusBadge = statusBadge;
    }

    public List<SubjectAttendanceStatsDto> getSubjectStats() {
        return subjectStats;
    }

    public void setSubjectStats(List<SubjectAttendanceStatsDto> subjectStats) {
        this.subjectStats = subjectStats;
    }
}
