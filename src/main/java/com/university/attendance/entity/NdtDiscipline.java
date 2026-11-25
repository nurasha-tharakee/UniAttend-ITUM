package com.university.attendance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ndt_disciplines")
public class NdtDiscipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    /**
     * Configurable by administrator (not hardcoded/fabricated).
     */
    @Column(length = 30)
    private String code;

    /**
     * Configurable student ID pattern/hint set by administrator.
     */
    @Column(name = "student_id_format", length = 80)
    private String studentIdFormat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id")
    private AcademicDivision division;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    public NdtDiscipline() {
    }

    public NdtDiscipline(String name, AcademicDivision division, int displayOrder) {
        this.name = name;
        this.division = division;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStudentIdFormat() { return studentIdFormat; }
    public void setStudentIdFormat(String studentIdFormat) { this.studentIdFormat = studentIdFormat; }

    public AcademicDivision getDivision() { return division; }
    public void setDivision(AcademicDivision division) { this.division = division; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
