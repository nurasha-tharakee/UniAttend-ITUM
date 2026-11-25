package com.university.attendance.repository;

import com.university.attendance.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<AcademicYear> findAllByOrderByNameDesc();
    List<AcademicYear> findByActiveTrueOrderByNameDesc();
}
