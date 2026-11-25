package com.university.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class SessionCreateRequest {

    @NotNull(message = "Please select a subject")
    private Long subjectId;

    @NotBlank(message = "Session title or topic is required")
    private String sessionName;

    @NotNull(message = "Session date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate sessionDate;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    public SessionCreateRequest() {
        this.sessionDate = LocalDate.now();
        this.startTime = LocalTime.now().withSecond(0).withNano(0);
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
}
