package com.university.attendance.repository;

import com.university.attendance.entity.NdtDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NdtDisciplineRepository extends JpaRepository<NdtDiscipline, Long> {
    Optional<NdtDiscipline> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<NdtDiscipline> findAllByOrderByDisplayOrderAscNameAsc();
    List<NdtDiscipline> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
