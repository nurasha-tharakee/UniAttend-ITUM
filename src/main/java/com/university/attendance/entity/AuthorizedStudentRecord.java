package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "authorized_student_records")
public class AuthorizedStudentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id_number", nullable = false, unique = true, length = 40)
    private String studentIdNumber;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "institutional_email", nullable = false, length = 120)
    private String institutionalEmail;

    @Column(length = 150)
    private String discipline;

    @Column(name = "batch_intake", length = 80)
    private String batchIntake;

    @Column(length = 120)
    private String programme = "National Diploma in Technology (NDT)";

    @Column(name = "academic_status", length = 40)
    private String academicStatus = "ACTIVE";

    @Column(name = "demonstration_record", nullable = false)
    private boolean demonstrationRecord = false;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private LocalDateTime importedAt;

    public AuthorizedStudentRecord() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.importedAt == null) {
            this.importedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentIdNumber() { return studentIdNumber; }
    public void setStudentIdNumber(String studentIdNumber) { this.studentIdNumber = studentIdNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getInstitutionalEmail() { return institutionalEmail; }
    public void setInstitutionalEmail(String institutionalEmail) { this.institutionalEmail = institutionalEmail; }

    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }

    public String getBatchIntake() { return batchIntake; }
    public void setBatchIntake(String batchIntake) { this.batchIntake = batchIntake; }

    public String getProgramme() { return programme; }
    public void setProgramme(String programme) { this.programme = programme; }

    public String getAcademicStatus() { return academicStatus; }
    public void setAcademicStatus(String academicStatus) { this.academicStatus = academicStatus; }

    public boolean isDemonstrationRecord() { return demonstrationRecord; }
    public void setDemonstrationRecord(boolean demonstrationRecord) { this.demonstrationRecord = demonstrationRecord; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
}
