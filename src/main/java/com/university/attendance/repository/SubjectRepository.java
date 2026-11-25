package com.university.attendance.repository;

import com.university.attendance.entity.Subject;
import com.university.attendance.entity.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    List<Subject> findAllByOrderByCodeAsc();

    List<Subject> findByStatusOrderByCodeAsc(SubjectStatus status);

    List<Subject> findByLecturerId(Long lecturerId);

    List<Subject> findByLecturerUserUsername(String username);
}
