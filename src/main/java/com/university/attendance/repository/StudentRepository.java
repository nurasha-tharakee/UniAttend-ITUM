package com.university.attendance.repository;

import com.university.attendance.entity.RegistrationStatus;
import com.university.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserUsername(String username);

    Optional<Student> findByStudentIdNumber(String studentIdNumber);

    Optional<Student> findByStudentIdNumberIgnoreCase(String studentIdNumber);

    boolean existsByStudentIdNumber(String studentIdNumber);

    boolean existsByStudentIdNumberIgnoreCase(String studentIdNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Student> findAllByOrderByFullNameAsc();

    List<Student> findByRegistrationStatusOrderByCreatedAtDesc(RegistrationStatus status);

    List<Student> findByRegistrationStatusInOrderByCreatedAtDesc(List<RegistrationStatus> statuses);

    long countByRegistrationStatus(RegistrationStatus status);

    long countByRegistrationStatusIn(List<RegistrationStatus> statuses);

    long countByDepartmentIgnoreCase(String department);

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.studentIdNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.department) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY s.fullName ASC")
    List<Student> searchStudents(@Param("query") String query);
}
