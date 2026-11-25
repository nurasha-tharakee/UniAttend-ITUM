package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String semester;

    @Column(name = "academic_year", length = 30)
    private String academicYear;

    @Column(name = "batch_intake", length = 80)
    private String batchIntake;

    @Column(name = "credit_value")
    private Integer creditValue;

    @Column(name = "common_subject")
    private Boolean commonSubject = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SubjectStatus status = SubjectStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "subject_disciplines",
            joinColumns = @JoinColumn(name = "subject_id"),
            inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    private Set<NdtDiscipline> disciplines = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lecturer_id")
    private Lecturer lecturer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Subject() {
    }

    public Subject(String code, String name, String department, String semester, Lecturer lecturer) {
        this.code = code;
        this.name = name;
        this.department = department;
        this.semester = semester;
        this.lecturer = lecturer;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public void setLecturer(Lecturer lecturer) {
        this.lecturer = lecturer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getBatchIntake() { return batchIntake; }
    public void setBatchIntake(String batchIntake) { this.batchIntake = batchIntake; }

    public Integer getCreditValue() { return creditValue; }
    public void setCreditValue(Integer creditValue) { this.creditValue = creditValue; }

    public boolean isCommonSubject() { return Boolean.TRUE.equals(commonSubject); }
    public void setCommonSubject(Boolean commonSubject) { this.commonSubject = commonSubject != null ? commonSubject : false; }

    public SubjectStatus getStatus() { return status != null ? status : SubjectStatus.ACTIVE; }
    public void setStatus(SubjectStatus status) { this.status = status; }

    public Set<NdtDiscipline> getDisciplines() { return disciplines; }
    public void setDisciplines(Set<NdtDiscipline> disciplines) {
        this.disciplines = disciplines != null ? disciplines : new HashSet<>();
    }
}
