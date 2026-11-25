package com.university.attendance.repository;

import com.university.attendance.entity.AuthorizedStudentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizedStudentRecordRepository extends JpaRepository<AuthorizedStudentRecord, Long> {
    Optional<AuthorizedStudentRecord> findByStudentIdNumberIgnoreCase(String studentIdNumber);
    boolean existsByStudentIdNumberIgnoreCase(String studentIdNumber);
    List<AuthorizedStudentRecord> findAllByOrderByImportedAtDesc();
}
