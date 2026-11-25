package com.university.attendance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "batch_intakes")
public class BatchIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "academic_year", length = 30)
    private String academicYear;

    @Column(name = "student_group", length = 50)
    private String studentGroup;

    @Column(nullable = false)
    private boolean active = true;

    public BatchIntake() {
    }

    public BatchIntake(String name, String academicYear, String studentGroup) {
        this.name = name;
        this.academicYear = academicYear;
        this.studentGroup = studentGroup;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getStudentGroup() { return studentGroup; }
    public void setStudentGroup(String studentGroup) { this.studentGroup = studentGroup; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
