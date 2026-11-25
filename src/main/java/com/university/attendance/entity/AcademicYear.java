package com.university.attendance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "is_current", nullable = false)
    private boolean currentYear = false;

    @Column(nullable = false)
    private boolean active = true;

    public AcademicYear() {
    }

    public AcademicYear(String name, boolean currentYear) {
        this.name = name;
        this.currentYear = currentYear;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isCurrentYear() { return currentYear; }
    public void setCurrentYear(boolean currentYear) { this.currentYear = currentYear; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
