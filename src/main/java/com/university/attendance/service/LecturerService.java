package com.university.attendance.service;

import com.university.attendance.dto.LecturerRegisterRequest;
import com.university.attendance.entity.AcademicDivision;
import com.university.attendance.entity.Lecturer;
import com.university.attendance.entity.NdtDiscipline;
import com.university.attendance.entity.Role;
import com.university.attendance.entity.Subject;
import com.university.attendance.entity.User;
import com.university.attendance.repository.AcademicDivisionRepository;
import com.university.attendance.repository.LecturerRepository;
import com.university.attendance.repository.NdtDisciplineRepository;
import com.university.attendance.repository.SubjectRepository;
import com.university.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class LecturerService {

    private final LecturerRepository lecturerRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicDivisionRepository divisionRepository;
    private final NdtDisciplineRepository disciplineRepository;
    private final PasswordEncoder passwordEncoder;

    public LecturerService(LecturerRepository lecturerRepository,
                           UserRepository userRepository,
                           SubjectRepository subjectRepository,
                           AcademicDivisionRepository divisionRepository,
                           NdtDisciplineRepository disciplineRepository,
                           PasswordEncoder passwordEncoder) {
        this.lecturerRepository = lecturerRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.divisionRepository = divisionRepository;
        this.disciplineRepository = disciplineRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Lecturer registerLecturer(LecturerRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already in use.");
        }
        if (lecturerRepository.existsByEmployeeId(request.getEmployeeId().trim())) {
            throw new IllegalArgumentException("Employee ID '" + request.getEmployeeId() + "' is already registered.");
        }
        if (lecturerRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email address '" + request.getEmail() + "' is already registered.");
        }

        User user = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_LECTURER
        );

        AcademicDivision division = request.getDivisionId() != null
                ? divisionRepository.findById(request.getDivisionId()).orElse(null)
                : null;
        Set<NdtDiscipline> disciplines = request.getDisciplineIds() != null && !request.getDisciplineIds().isEmpty()
                ? new HashSet<>(disciplineRepository.findAllById(request.getDisciplineIds()))
                : new HashSet<>();

        String deptText = request.getDepartment() != null && !request.getDepartment().isBlank()
                ? request.getDepartment().trim()
                : (division != null ? division.getName() : "");

        Lecturer lecturer = new Lecturer(
                user,
                request.getEmployeeId().trim(),
                request.getFullName().trim(),
                request.getEmail().trim(),
                deptText
        );
        lecturer.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        lecturer.setContactNumber(request.getContactNumber() != null ? request.getContactNumber().trim() : null);
        lecturer.setAcademicDivision(division);
        lecturer.setDesignation(request.getDesignation() != null ? request.getDesignation().trim() : null);
        lecturer.setAssociatedDisciplines(disciplines);
        lecturer.setActive(true);

        return lecturerRepository.save(lecturer);
    }

    @Transactional(readOnly = true)
    public List<Lecturer> getAllLecturers() {
        return lecturerRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Lecturer> searchLecturers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllLecturers();
        }
        return lecturerRepository.searchLecturers(query.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Lecturer> getLecturerById(Long id) {
        return lecturerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Lecturer> getLecturerByUsername(String username) {
        return lecturerRepository.findByUserUsername(username);
    }

    public Lecturer updateLecturer(Long id, String fullName, String email, String department) {
        return updateLecturerAdmin(id, null, fullName, email, null, null, null, department, null, true);
    }

    public Lecturer updateLecturerAdmin(Long id, String title, String fullName, String email,
                                        String contactNumber, Long divisionId, String designation,
                                        String department, List<Long> disciplineIds, boolean active) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with ID: " + id));

        if (!lecturer.getEmail().equalsIgnoreCase(email.trim()) && lecturerRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email address '" + email + "' is already in use by another lecturer.");
        }

        AcademicDivision division = divisionId != null ? divisionRepository.findById(divisionId).orElse(null) : null;
        Set<NdtDiscipline> disciplines = disciplineIds != null && !disciplineIds.isEmpty()
                ? new HashSet<>(disciplineRepository.findAllById(disciplineIds))
                : new HashSet<>();

        lecturer.setTitle(title != null ? title.trim() : null);
        lecturer.setFullName(fullName.trim());
        lecturer.setEmail(email.trim());
        lecturer.setContactNumber(contactNumber != null ? contactNumber.trim() : null);
        lecturer.setAcademicDivision(division);
        lecturer.setDesignation(designation != null ? designation.trim() : null);
        lecturer.setAssociatedDisciplines(disciplines);
        lecturer.setActive(active);
        if (lecturer.getUser() != null) {
            lecturer.getUser().setEnabled(active);
        }
        String deptText = department != null && !department.isBlank()
                ? department.trim()
                : (division != null ? division.getName() : lecturer.getDepartment());
        lecturer.setDepartment(deptText);

        return lecturerRepository.save(lecturer);
    }

    public Lecturer updateLecturerProfile(String username, String title, String fullName,
                                          String contactNumber, String email,
                                          String currentPassword, String newPassword) {
        Lecturer lecturer = lecturerRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer profile not found."));

        if (!lecturer.getEmail().equalsIgnoreCase(email.trim()) && lecturerRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email address '" + email + "' is already in use.");
        }

        lecturer.setTitle(title != null ? title.trim() : lecturer.getTitle());
        lecturer.setFullName(fullName.trim());
        lecturer.setContactNumber(contactNumber != null ? contactNumber.trim() : null);
        lecturer.setEmail(email.trim());

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, lecturer.getUser().getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters.");
            }
            lecturer.getUser().setPassword(passwordEncoder.encode(newPassword));
        }

        return lecturerRepository.save(lecturer);
    }

    public void toggleLecturerActive(Long id) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with ID: " + id));
        boolean next = !lecturer.isActive();
        lecturer.setActive(next);
        if (lecturer.getUser() != null) {
            lecturer.getUser().setEnabled(next);
        }
        lecturerRepository.save(lecturer);
    }

    @Transactional(readOnly = true)
    public List<Lecturer> filterLecturers(String search, Long disciplineId, Long divisionId, String status) {
        return searchLecturers(search).stream()
                .filter(l -> divisionId == null || (l.getAcademicDivision() != null && l.getAcademicDivision().getId().equals(divisionId)))
                .filter(l -> disciplineId == null || l.getAssociatedDisciplines().stream().anyMatch(d -> d.getId().equals(disciplineId)))
                .filter(l -> status == null || status.isBlank()
                        || ("ACTIVE".equalsIgnoreCase(status) && l.isActive())
                        || ("INACTIVE".equalsIgnoreCase(status) && !l.isActive()))
                .toList();
    }

    public void deleteLecturer(Long id) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with ID: " + id));

        // Unlink lecturer from subjects
        List<Subject> subjects = subjectRepository.findByLecturerId(id);
        for (Subject subject : subjects) {
            subject.setLecturer(null);
            subjectRepository.save(subject);
        }

        lecturerRepository.delete(lecturer);
    }

    @Transactional(readOnly = true)
    public long getTotalLecturersCount() {
        return lecturerRepository.count();
    }
}
