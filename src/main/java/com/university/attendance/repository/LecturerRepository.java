package com.university.attendance.repository;

import com.university.attendance.entity.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    Optional<Lecturer> findByUserUsername(String username);

    Optional<Lecturer> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    List<Lecturer> findAllByOrderByFullNameAsc();

    @Query("SELECT l FROM Lecturer l WHERE " +
           "LOWER(l.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.employeeId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.department) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY l.fullName ASC")
    List<Lecturer> searchLecturers(@Param("query") String query);
}
