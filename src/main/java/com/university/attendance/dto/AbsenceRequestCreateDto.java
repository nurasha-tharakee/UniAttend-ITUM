package com.university.attendance.dto;

import com.university.attendance.entity.AbsenceReasonCategory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class AbsenceRequestCreateDto {

    private Long subjectId;
    private Long sessionId;
    private AbsenceReasonCategory reasonCategory;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private String explanation;
    private MultipartFile document;

    public AbsenceRequestCreateDto() {
        this.startDate = LocalDate.now();
        this.endDate = LocalDate.now();
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public AbsenceReasonCategory getReasonCategory() {
        return reasonCategory;
    }

    public void setReasonCategory(AbsenceReasonCategory reasonCategory) {
        this.reasonCategory = reasonCategory;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public MultipartFile getDocument() {
        return document;
    }

    public void setDocument(MultipartFile document) {
        this.document = document;
    }
}
