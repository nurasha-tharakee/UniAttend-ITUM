package com.university.attendance.dto;

public class SubjectAttendanceStatsDto {
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private String lecturerName;
    private long totalConducted;
    private long presentCount;
    private long absentCount;
    private double percentage;
    private String statusBadge; // success (>= 80%), warning (75-79%), danger (< 75%)

    public SubjectAttendanceStatsDto() {
    }

    public SubjectAttendanceStatsDto(Long subjectId, String subjectCode, String subjectName, String lecturerName, long totalConducted, long presentCount) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.lecturerName = lecturerName;
        this.totalConducted = totalConducted;
        this.presentCount = presentCount;
        this.absentCount = Math.max(0, totalConducted - presentCount);
        if (totalConducted > 0) {
            this.percentage = Math.round((presentCount * 100.0 / totalConducted) * 10.0) / 10.0;
        } else {
            this.percentage = 100.0; // No classes yet
        }
        if (this.percentage >= 80.0) {
            this.statusBadge = "success";
        } else if (this.percentage >= 75.0) {
            this.statusBadge = "warning";
        } else {
            this.statusBadge = "danger";
        }
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
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

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getStatusBadge() {
        return statusBadge;
    }

    public void setStatusBadge(String statusBadge) {
        this.statusBadge = statusBadge;
    }
}
