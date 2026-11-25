package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "student_id_number", nullable = false, unique = true, length = 30)
    private String studentIdNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(name = "academic_year", length = 30)
    private String academicYear;

    @Column(name = "personal_email", length = 120)
    private String personalEmail;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(length = 120)
    private String campus = "Institute of Technology, University of Moratuwa";

    @Column(length = 120)
    private String programme = "National Diploma in Technology (NDT)";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discipline_id")
    private NdtDiscipline discipline;

    @Column(name = "batch_intake", length = 80)
    private String batchIntake;

    @Column(name = "year_of_study", length = 30)
    private String yearOfStudy;

    @Column(length = 50)
    private String semester;

    @Column(name = "student_group", length = 50)
    private String studentGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", length = 30)
    private RegistrationStatus registrationStatus = RegistrationStatus.APPROVED;

    @Column(name = "identity_verified")
    private Boolean identityVerified = false;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "verification_otp", length = 16)
    private String verificationOtp;

    @Column(name = "verification_expires_at")
    private LocalDateTime verificationExpiresAt;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Student() {
    }

    public Student(User user, String studentIdNumber, String fullName, String email, String department, String academicYear) {
        this.user = user;
        this.studentIdNumber = studentIdNumber;
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.academicYear = academicYear;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getCampus() { return campus != null && !campus.isBlank() ? campus : "Institute of Technology, University of Moratuwa"; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getProgramme() { return programme != null && !programme.isBlank() ? programme : "National Diploma in Technology (NDT)"; }
    public void setProgramme(String programme) { this.programme = programme; }

    public NdtDiscipline getDiscipline() { return discipline; }
    public void setDiscipline(NdtDiscipline discipline) {
        this.discipline = discipline;
        if (discipline != null) {
            this.department = discipline.getName();
        }
    }

    public String getBatchIntake() { return batchIntake; }
    public void setBatchIntake(String batchIntake) { this.batchIntake = batchIntake; }

    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getStudentGroup() { return studentGroup; }
    public void setStudentGroup(String studentGroup) { this.studentGroup = studentGroup; }

    public RegistrationStatus getRegistrationStatus() { return registrationStatus != null ? registrationStatus : RegistrationStatus.APPROVED; }
    public void setRegistrationStatus(RegistrationStatus registrationStatus) { this.registrationStatus = registrationStatus; }

    public boolean isIdentityVerified() { return Boolean.TRUE.equals(identityVerified); }
    public void setIdentityVerified(Boolean identityVerified) { this.identityVerified = identityVerified != null ? identityVerified : false; }

    public boolean isEmailVerified() { return Boolean.TRUE.equals(emailVerified); }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified != null ? emailVerified : false; }

    public String getVerificationOtp() { return verificationOtp; }
    public void setVerificationOtp(String verificationOtp) { this.verificationOtp = verificationOtp; }

    public LocalDateTime getVerificationExpiresAt() { return verificationExpiresAt; }
    public void setVerificationExpiresAt(LocalDateTime verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
