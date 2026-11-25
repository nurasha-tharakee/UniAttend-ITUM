package com.university.attendance.repository;

import com.university.attendance.entity.BatchIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchIntakeRepository extends JpaRepository<BatchIntake, Long> {
    Optional<BatchIntake> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<BatchIntake> findAllByOrderByNameDesc();
    List<BatchIntake> findByActiveTrueOrderByNameDesc();
}
