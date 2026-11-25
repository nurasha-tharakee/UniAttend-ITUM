package com.university.attendance.repository;

import com.university.attendance.entity.AbsenceRequest;
import com.university.attendance.entity.AbsenceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {

    List<AbsenceRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<AbsenceRequest> findByStudentUserUsernameOrderByCreatedAtDesc(String username);

    List<AbsenceRequest> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);

    List<AbsenceRequest> findBySubjectLecturerUserUsernameOrderByCreatedAtDesc(String username);

    List<AbsenceRequest> findByStatusOrderByCreatedAtDesc(AbsenceRequestStatus status);

    List<AbsenceRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(AbsenceRequestStatus status);

    long countBySubjectLecturerUserUsernameAndStatus(String username, AbsenceRequestStatus status);

    long countByStudentUserUsernameAndStatus(String username, AbsenceRequestStatus status);
}
