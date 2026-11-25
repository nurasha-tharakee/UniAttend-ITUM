package com.university.attendance.repository;

import com.university.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<Attendance> findBySessionIdOrderByMarkedAtAsc(Long sessionId);

    List<Attendance> findByStudentIdOrderByMarkedAtDesc(Long studentId);

    List<Attendance> findByStudentUserUsernameOrderByMarkedAtDesc(String username);

    long countBySessionId(Long sessionId);

    long countByStudentId(Long studentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.session.subject.id = :subjectId AND a.session.createdAt >= :enrolledAt")
    long countAttendedSessionsForStudentAndSubject(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("enrolledAt") LocalDateTime enrolledAt
    );

    @Query("SELECT a FROM Attendance a WHERE a.session.subject.id = :subjectId ORDER BY a.markedAt DESC")
    List<Attendance> findBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT a FROM Attendance a WHERE " +
           "(:subjectId IS NULL OR a.session.subject.id = :subjectId) AND " +
           "(:studentId IS NULL OR a.student.id = :studentId) AND " +
           "(:startDate IS NULL OR a.session.sessionDate >= :startDate) AND " +
           "(:endDate IS NULL OR a.session.sessionDate <= :endDate) " +
           "ORDER BY a.markedAt DESC")
    List<Attendance> filterAttendance(
            @Param("subjectId") Long subjectId,
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Attendance> findAllByOrderByMarkedAtDesc();
}
