package com.university.attendance.dto;

public class AttendanceResponse {
    private boolean success;
    private String message;
    private String subjectCode;
    private String subjectName;
    private String sessionName;
    private String markedAt;
    private Double currentPercentage;

    public AttendanceResponse() {
    }

    public AttendanceResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AttendanceResponse(boolean success, String message, String subjectCode, String subjectName, String sessionName, String markedAt, Double currentPercentage) {
        this.success = success;
        this.message = message;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.sessionName = sessionName;
        this.markedAt = markedAt;
        this.currentPercentage = currentPercentage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(String markedAt) {
        this.markedAt = markedAt;
    }

    public Double getCurrentPercentage() {
        return currentPercentage;
    }

    public void setCurrentPercentage(Double currentPercentage) {
        this.currentPercentage = currentPercentage;
    }
}
