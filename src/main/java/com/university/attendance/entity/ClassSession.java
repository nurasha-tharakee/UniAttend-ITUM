package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "class_sessions")
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @Column(name = "session_name", nullable = false, length = 150)
    private String sessionName;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "qr_token", unique = true, length = 100)
    private String qrToken;

    @Column(name = "qr_token_expires_at")
    private LocalDateTime qrTokenExpiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ClassSession() {
    }

    public ClassSession(Subject subject, Lecturer lecturer, String sessionName, LocalDate sessionDate, LocalTime startTime) {
        this.subject = subject;
        this.lecturer = lecturer;
        this.sessionName = sessionName;
        this.sessionDate = sessionDate;
        this.startTime = startTime != null ? startTime.withNano(0) : LocalTime.now().withNano(0);
        this.active = true;
        this.createdAt = LocalDateTime.now().withNano(0);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now().withNano(0);
        }
        if (this.startTime != null) {
            this.startTime = this.startTime.withNano(0);
        }
        if (this.endTime != null) {
            this.endTime = this.endTime.withNano(0);
        }
        if (this.qrTokenExpiresAt != null) {
            this.qrTokenExpiresAt = this.qrTokenExpiresAt.withNano(0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.startTime != null) {
            this.startTime = this.startTime.withNano(0);
        }
        if (this.endTime != null) {
            this.endTime = this.endTime.withNano(0);
        }
        if (this.qrTokenExpiresAt != null) {
            this.qrTokenExpiresAt = this.qrTokenExpiresAt.withNano(0);
        }
    }

    public boolean isQrExpired() {
        return qrTokenExpiresAt == null || LocalDateTime.now().isAfter(qrTokenExpiresAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public void setLecturer(Lecturer lecturer) {
        this.lecturer = lecturer;
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
        this.startTime = startTime != null ? startTime.withNano(0) : null;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime != null ? endTime.withNano(0) : null;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public LocalDateTime getQrTokenExpiresAt() {
        return qrTokenExpiresAt;
    }

    public void setQrTokenExpiresAt(LocalDateTime qrTokenExpiresAt) {
        this.qrTokenExpiresAt = qrTokenExpiresAt != null ? qrTokenExpiresAt.withNano(0) : null;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? createdAt.withNano(0) : null;
    }
}
