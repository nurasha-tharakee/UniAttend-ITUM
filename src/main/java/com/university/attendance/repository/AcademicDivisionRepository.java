package com.university.attendance.repository;

import com.university.attendance.entity.AcademicDivision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicDivisionRepository extends JpaRepository<AcademicDivision, Long> {
    Optional<AcademicDivision> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<AcademicDivision> findAllByOrderByNameAsc();
    List<AcademicDivision> findByActiveTrueOrderByNameAsc();
}
