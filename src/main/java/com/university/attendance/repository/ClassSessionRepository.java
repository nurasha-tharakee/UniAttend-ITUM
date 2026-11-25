package com.university.attendance.repository;

import com.university.attendance.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    Optional<ClassSession> findByQrToken(String qrToken);

    List<ClassSession> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);

    List<ClassSession> findByLecturerIdOrderByCreatedAtDesc(Long lecturerId);

    List<ClassSession> findByLecturerUserUsernameOrderByCreatedAtDesc(String username);

    List<ClassSession> findBySubjectIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Long subjectId, LocalDateTime after);

    long countBySubjectId(Long subjectId);

    long countBySubjectIdAndCreatedAtGreaterThanEqual(Long subjectId, LocalDateTime after);

    List<ClassSession> findAllByOrderByCreatedAtDesc();
}
