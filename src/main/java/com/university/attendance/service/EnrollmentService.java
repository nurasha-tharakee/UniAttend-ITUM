package com.university.attendance.service;

import com.university.attendance.entity.Enrollment;
import com.university.attendance.entity.Student;
import com.university.attendance.entity.Subject;
import com.university.attendance.repository.EnrollmentRepository;
import com.university.attendance.repository.StudentRepository;
import com.university.attendance.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             SubjectRepository subjectRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    public Enrollment enrollStudent(Long studentId, Long subjectId) {
        if (enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            throw new IllegalArgumentException("Student is already enrolled in this subject.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + subjectId));

        Enrollment enrollment = new Enrollment(student, subject);
        return enrollmentRepository.save(enrollment);
    }

    public List<Enrollment> enrollMultipleStudents(List<Long> studentIds, Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + subjectId));

        List<Enrollment> enrolledList = new ArrayList<>();
        for (Long studentId : studentIds) {
            if (!enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
                studentRepository.findById(studentId).ifPresent(student -> {
                    Enrollment enrollment = new Enrollment(student, subject);
                    enrolledList.add(enrollmentRepository.save(enrollment));
                });
            }
        }
        return enrolledList;
    }

    public void unenrollStudent(Long studentId, Long subjectId) {
        enrollmentRepository.deleteByStudentIdAndSubjectId(studentId, subjectId);
    }

    public void removeEnrollment(Long enrollmentId) {
        enrollmentRepository.deleteById(enrollmentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsBySubject(Long subjectId) {
        return enrollmentRepository.findBySubjectId(subjectId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getEnrollmentsByStudentUsername(String username) {
        return enrollmentRepository.findByStudentUserUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long studentId, Long subjectId) {
        return enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public Optional<Enrollment> getEnrollment(Long studentId, Long subjectId) {
        return enrollmentRepository.findByStudentIdAndSubjectId(studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public long getEnrolledStudentsCount(Long subjectId) {
        return enrollmentRepository.countBySubjectId(subjectId);
    }
}
