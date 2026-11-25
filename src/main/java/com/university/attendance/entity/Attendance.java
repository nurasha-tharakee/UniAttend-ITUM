package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(name = "uk_session_student", columnNames = {"session_id", "student_id"})
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @Column(nullable = false, length = 20)
    private String status = "PRESENT";

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public Attendance() {
    }

    public Attendance(ClassSession session, Student student, String ipAddress, String deviceInfo) {
        this.session = session;
        this.student = student;
        this.markedAt = LocalDateTime.now();
        this.status = "PRESENT";
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
    }

    public Attendance(ClassSession session, Student student, String ipAddress, String deviceInfo, Double latitude, Double longitude) {
        this.session = session;
        this.student = student;
        this.markedAt = LocalDateTime.now();
        this.status = "PRESENT";
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @PrePersist
    protected void onCreate() {
        if (this.markedAt == null) {
            this.markedAt = LocalDateTime.now().withNano(0);
        } else {
            this.markedAt = this.markedAt.withNano(0);
        }
        if (this.status == null) {
            this.status = "PRESENT";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassSession getSession() {
        return session;
    }

    public void setSession(ClassSession session) {
        this.session = session;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
