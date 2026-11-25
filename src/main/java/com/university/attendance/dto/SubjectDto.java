package com.university.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class SubjectDto {

    private Long id;

    @NotBlank(message = "Subject code is required")
    @Size(max = 30)
    private String code;

    @NotBlank(message = "Subject name is required")
    @Size(max = 150)
    private String name;

    private String department;

    private List<Long> disciplineIds = new ArrayList<>();

    private boolean commonSubject = false;

    private String academicYear;

    private String semester;

    private String batchIntake;

    private Integer creditValue;

    private Long lecturerId;

    private String status = "ACTIVE";

    public SubjectDto() {
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

    public Long getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(Long lecturerId) {
        this.lecturerId = lecturerId;
    }

    public List<Long> getDisciplineIds() { return disciplineIds; }
    public void setDisciplineIds(List<Long> disciplineIds) { this.disciplineIds = disciplineIds != null ? disciplineIds : new ArrayList<>(); }

    public boolean isCommonSubject() { return commonSubject; }
    public void setCommonSubject(boolean commonSubject) { this.commonSubject = commonSubject; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getBatchIntake() { return batchIntake; }
    public void setBatchIntake(String batchIntake) { this.batchIntake = batchIntake; }

    public Integer getCreditValue() { return creditValue; }
    public void setCreditValue(Integer creditValue) { this.creditValue = creditValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
