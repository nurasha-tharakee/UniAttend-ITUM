package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "absence_requests")
public class AbsenceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 40)
    private AbsenceReasonCategory reasonCategory;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 2000)
    private String explanation;

    @Column(name = "attachment_path", length = 300)
    private String attachmentPath;

    @Column(name = "attachment_original_name", length = 200)
    private String attachmentOriginalName;

    @Column(name = "attachment_type", length = 50)
    private String attachmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AbsenceRequestStatus status = AbsenceRequestStatus.PENDING;

    @Column(name = "review_comments", length = 2000)
    private String reviewComments;

    @Column(name = "reviewed_by", length = 60)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AbsenceRequest() {
    }

    public AbsenceRequest(Student student, Subject subject, ClassSession session,
                          AbsenceReasonCategory reasonCategory, LocalDate startDate, LocalDate endDate,
                          String explanation) {
        this.student = student;
        this.subject = subject;
        this.session = session;
        this.reasonCategory = reasonCategory;
        this.startDate = startDate;
        this.endDate = endDate;
        this.explanation = explanation;
        this.status = AbsenceRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now().withNano(0);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now().withNano(0);
        }
        if (this.status == null) {
            this.status = AbsenceRequestStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public ClassSession getSession() {
        return session;
    }

    public void setSession(ClassSession session) {
        this.session = session;
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

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getAttachmentOriginalName() {
        return attachmentOriginalName;
    }

    public void setAttachmentOriginalName(String attachmentOriginalName) {
        this.attachmentOriginalName = attachmentOriginalName;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public AbsenceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AbsenceRequestStatus status) {
        this.status = status;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt != null ? reviewedAt.withNano(0) : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? createdAt.withNano(0) : null;
    }
}
