package com.university.attendance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lecturers")
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_id", nullable = false, unique = true, length = 30)
    private String employeeId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(length = 20)
    private String title;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id")
    private AcademicDivision academicDivision;

    @Column(length = 80)
    private String designation;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "lecturer_disciplines",
            joinColumns = @JoinColumn(name = "lecturer_id"),
            inverseJoinColumns = @JoinColumn(name = "discipline_id")
    )
    private Set<NdtDiscipline> associatedDisciplines = new HashSet<>();

    @Column
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Lecturer() {
    }

    public Lecturer(User user, String employeeId, String fullName, String email, String department) {
        this.user = user;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.email = email;
        this.department = department;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public AcademicDivision getAcademicDivision() { return academicDivision; }
    public void setAcademicDivision(AcademicDivision academicDivision) { this.academicDivision = academicDivision; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Set<NdtDiscipline> getAssociatedDisciplines() { return associatedDisciplines; }
    public void setAssociatedDisciplines(Set<NdtDiscipline> associatedDisciplines) {
        this.associatedDisciplines = associatedDisciplines != null ? associatedDisciplines : new HashSet<>();
    }

    public boolean isActive() { return active == null || active; }
    public void setActive(Boolean active) { this.active = active != null ? active : true; }
}
