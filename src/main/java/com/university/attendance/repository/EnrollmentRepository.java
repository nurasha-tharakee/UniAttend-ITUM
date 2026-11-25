package com.university.attendance.repository;

import com.university.attendance.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByStudentUserUsername(String username);

    List<Enrollment> findBySubjectId(Long subjectId);

    Optional<Enrollment> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);

    long countBySubjectId(Long subjectId);

    long countByStudentId(Long studentId);

    void deleteByStudentIdAndSubjectId(Long studentId, Long subjectId);
}
