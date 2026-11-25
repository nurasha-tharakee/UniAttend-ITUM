package com.university.attendance.service;

import com.university.attendance.dto.StudentRegisterRequest;
import com.university.attendance.entity.AuthorizedStudentRecord;
import com.university.attendance.entity.NdtDiscipline;
import com.university.attendance.entity.RegistrationStatus;
import com.university.attendance.entity.Role;
import com.university.attendance.entity.Student;
import com.university.attendance.entity.User;
import com.university.attendance.repository.AttendanceRepository;
import com.university.attendance.repository.AuthorizedStudentRecordRepository;
import com.university.attendance.repository.EnrollmentRepository;
import com.university.attendance.repository.NdtDisciplineRepository;
import com.university.attendance.repository.StudentRepository;
import com.university.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final NdtDisciplineRepository disciplineRepository;
    private final AuthorizedStudentRecordRepository authorizedRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          EnrollmentRepository enrollmentRepository,
                          AttendanceRepository attendanceRepository,
                          NdtDisciplineRepository disciplineRepository,
                          AuthorizedStudentRecordRepository authorizedRecordRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.disciplineRepository = disciplineRepository;
        this.authorizedRecordRepository = authorizedRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Student registerStudent(StudentRegisterRequest request) {
        validateUniqueStudentFields(request);

        User user = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT
        );
        user.setEnabled(true);

        NdtDiscipline discipline = resolveDiscipline(request.getDisciplineId(), request.getDepartment());
        String deptName = discipline != null ? discipline.getName()
                : (request.getDepartment() != null ? request.getDepartment().trim() : "");

        Student student = new Student(
                user,
                request.getStudentIdNumber().trim(),
                request.getFullName().trim(),
                request.getEmail().trim(),
                deptName,
                request.getAcademicYear() != null ? request.getAcademicYear().trim() : ""
        );
        applyExtendedStudentFields(student, request, discipline);
        student.setRegistrationStatus(RegistrationStatus.APPROVED);
        student.setIdentityVerified(true);
        student.setEmailVerified(true);
        student.setApprovedAt(LocalDateTime.now());

        return studentRepository.save(student);
    }

    public Student selfRegisterStudent(StudentRegisterRequest request) {
        if (request.getConfirmPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and Confirm Password do not match.");
        }
        validateUniqueStudentFields(request);

        NdtDiscipline discipline = resolveDiscipline(request.getDisciplineId(), request.getDepartment());
        if (discipline == null && (request.getDepartment() == null || request.getDepartment().isBlank())) {
            throw new IllegalArgumentException("Please select an NDT discipline.");
        }

        User user = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT
        );
        // Account remains disabled until email verification & admin approval
        user.setEnabled(false);

        String deptName = discipline != null ? discipline.getName() : request.getDepartment().trim();
        Student student = new Student(
                user,
                request.getStudentIdNumber().trim(),
                request.getFullName().trim(),
                request.getEmail().trim(),
                deptName,
                request.getAcademicYear() != null ? request.getAcademicYear().trim() : ""
        );
        applyExtendedStudentFields(student, request, discipline);

        // Verify against Authorized Student Database
        Optional<AuthorizedStudentRecord> authOpt = authorizedRecordRepository
                .findByStudentIdNumberIgnoreCase(request.getStudentIdNumber().trim());
        boolean matched = authOpt.isPresent()
                && authOpt.get().getInstitutionalEmail().equalsIgnoreCase(request.getEmail().trim())
                && authOpt.get().getFullName().equalsIgnoreCase(request.getFullName().trim());

        student.setIdentityVerified(matched);
        student.setEmailVerified(false);
        student.setRegistrationStatus(RegistrationStatus.PENDING_VERIFICATION);
        student.setVerificationOtp(String.format("%06d", secureRandom.nextInt(1_000_000)));
        student.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(30));

        return studentRepository.save(student);
    }

    private void validateUniqueStudentFields(StudentRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already in use.");
        }
        if (studentRepository.existsByStudentIdNumberIgnoreCase(request.getStudentIdNumber().trim())) {
            throw new IllegalArgumentException("Student ID Number '" + request.getStudentIdNumber() + "' is already registered.");
        }
        if (studentRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email address '" + request.getEmail() + "' is already registered.");
        }
    }

    private NdtDiscipline resolveDiscipline(Long disciplineId, String departmentName) {
        if (disciplineId != null) {
            return disciplineRepository.findById(disciplineId).orElse(null);
        }
        if (departmentName != null && !departmentName.isBlank()) {
            return disciplineRepository.findByNameIgnoreCase(departmentName.trim()).orElse(null);
        }
        return null;
    }

    private void applyExtendedStudentFields(Student student, StudentRegisterRequest request, NdtDiscipline discipline) {
        student.setDiscipline(discipline);
        student.setPersonalEmail(request.getPersonalEmail() != null && !request.getPersonalEmail().isBlank() ? request.getPersonalEmail().trim() : null);
        student.setContactNumber(request.getContactNumber() != null ? request.getContactNumber().trim() : null);
        student.setCampus("Institute of Technology, University of Moratuwa");
        student.setProgramme("National Diploma in Technology (NDT)");
        student.setBatchIntake(request.getBatchIntake() != null ? request.getBatchIntake().trim() : "");
        student.setYearOfStudy(request.getYearOfStudy() != null ? request.getYearOfStudy().trim() : "");
        student.setSemester(request.getSemester() != null ? request.getSemester().trim() : "");
        student.setStudentGroup(request.getStudentGroup() != null ? request.getStudentGroup().trim() : "");
    }

    public Student verifyEmailOtp(String studentIdNumber, String otp) {
        Student student = studentRepository.findByStudentIdNumberIgnoreCase(studentIdNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Student registration not found."));

        if (student.getRegistrationStatus() != RegistrationStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("This registration is already in status: " + student.getRegistrationStatus().getDisplayName());
        }
        if (student.getVerificationOtp() == null || !student.getVerificationOtp().equals(otp != null ? otp.trim() : "")) {
            throw new IllegalArgumentException("Invalid verification code (OTP).");
        }
        if (student.getVerificationExpiresAt() != null && LocalDateTime.now().isAfter(student.getVerificationExpiresAt())) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        student.setEmailVerified(true);
        student.setVerificationOtp(null);
        student.setVerificationExpiresAt(null);
        student.setRegistrationStatus(RegistrationStatus.PENDING_APPROVAL);
        return studentRepository.save(student);
    }

    public Student approveStudent(Long id, String adminUsername) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
        student.setRegistrationStatus(RegistrationStatus.APPROVED);
        student.setRejectionReason(null);
        student.setApprovedBy(adminUsername);
        student.setApprovedAt(LocalDateTime.now());
        if (student.getUser() != null) {
            student.getUser().setEnabled(true);
        }
        return studentRepository.save(student);
    }

    public Student rejectStudent(Long id, String reason, String adminUsername) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
        student.setRegistrationStatus(RegistrationStatus.REJECTED);
        student.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Rejected by administrator");
        student.setApprovedBy(adminUsername);
        student.setApprovedAt(LocalDateTime.now());
        if (student.getUser() != null) {
            student.getUser().setEnabled(false);
        }
        return studentRepository.save(student);
    }

    public void toggleStudentActive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
        if (student.getUser() != null) {
            student.getUser().setEnabled(!student.getUser().isEnabled());
        }
        studentRepository.save(student);
    }

    public Student updateStudentAdmin(Long id, String fullName, String email, String contactNumber,
                                      Long disciplineId, String batchIntake, String academicYear,
                                      String yearOfStudy, String semester, String studentGroup) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));

        if (!student.getEmail().equalsIgnoreCase(email.trim()) && studentRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Email address '" + email + "' is already in use by another student.");
        }

        // Prevent unauthorized changes to verified identity fields (studentIdNumber is immutable once verified)
        student.setFullName(fullName.trim());
        student.setEmail(email.trim());
        student.setContactNumber(contactNumber != null ? contactNumber.trim() : null);

        if (disciplineId != null) {
            NdtDiscipline disc = disciplineRepository.findById(disciplineId).orElse(null);
            if (disc != null) {
                student.setDiscipline(disc);
            }
        }
        student.setBatchIntake(batchIntake != null ? batchIntake.trim() : student.getBatchIntake());
        student.setAcademicYear(academicYear != null ? academicYear.trim() : student.getAcademicYear());
        student.setYearOfStudy(yearOfStudy != null ? yearOfStudy.trim() : student.getYearOfStudy());
        student.setSemester(semester != null ? semester.trim() : student.getSemester());
        student.setStudentGroup(studentGroup != null ? studentGroup.trim() : student.getStudentGroup());

        return studentRepository.save(student);
    }

    public Student updateStudentProfile(String username, String contactNumber, String personalEmail,
                                        String currentPassword, String newPassword) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found."));

        student.setContactNumber(contactNumber != null ? contactNumber.trim() : null);
        student.setPersonalEmail(personalEmail != null && !personalEmail.isBlank() ? personalEmail.trim() : null);

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, student.getUser().getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters.");
            }
            student.getUser().setPassword(passwordEncoder.encode(newPassword));
        }

        return studentRepository.save(student);
    }

    public int importAuthorizedStudentsCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a CSV file to import.");
        }
        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (firstLine) {
                    firstLine = false;
                    if (cols[0].toLowerCase().contains("student") || cols[0].toLowerCase().contains("id")) {
                        continue; // skip header row
                    }
                }
                if (cols.length < 3) continue;
                String stuId = cols[0].trim();
                String fullName = cols[1].trim();
                String email = cols[2].trim();
                if (stuId.isEmpty() || fullName.isEmpty() || email.isEmpty()) continue;

                AuthorizedStudentRecord rec = authorizedRecordRepository
                        .findByStudentIdNumberIgnoreCase(stuId)
                        .orElseGet(AuthorizedStudentRecord::new);
                rec.setStudentIdNumber(stuId);
                rec.setFullName(fullName);
                rec.setInstitutionalEmail(email);
                rec.setDiscipline(cols.length > 3 ? cols[3].trim() : "");
                rec.setBatchIntake(cols.length > 4 ? cols[4].trim() : "");
                rec.setProgramme(cols.length > 5 && !cols[5].isBlank() ? cols[5].trim() : "National Diploma in Technology (NDT)");
                rec.setAcademicStatus(cols.length > 6 && !cols[6].isBlank() ? cols[6].trim() : "ACTIVE");
                rec.setDemonstrationRecord(false);
                authorizedRecordRepository.save(rec);
                imported++;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage());
        }
        return imported;
    }

    @Transactional(readOnly = true)
    public List<AuthorizedStudentRecord> getAllAuthorizedRecords() {
        return authorizedRecordRepository.findAllByOrderByImportedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Student> getPendingVerificationStudents() {
        return studentRepository.findByRegistrationStatusInOrderByCreatedAtDesc(
                List.of(RegistrationStatus.PENDING_VERIFICATION, RegistrationStatus.PENDING_APPROVAL, RegistrationStatus.REJECTED)
        );
    }

    @Transactional(readOnly = true)
    public long countPendingRegistrations() {
        return studentRepository.countByRegistrationStatusIn(
                List.of(RegistrationStatus.PENDING_VERIFICATION, RegistrationStatus.PENDING_APPROVAL)
        );
    }

    @Transactional(readOnly = true)
    public List<Student> filterStudents(String search, String discipline, String status) {
        return searchStudents(search).stream()
                .filter(s -> discipline == null || discipline.isBlank()
                        || (s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(discipline.trim())))
                .filter(s -> status == null || status.isBlank()
                        || s.getRegistrationStatus().name().equalsIgnoreCase(status.trim()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllStudents();
        }
        return studentRepository.searchStudents(query.trim());
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username);
    }

    public Student updateStudent(Long id, String fullName, String email, String department, String academicYear) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));

        if (!student.getEmail().equalsIgnoreCase(email) && studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address '" + email + "' is already in use by another student.");
        }

        student.setFullName(fullName.trim());
        student.setEmail(email.trim());
        student.setDepartment(department != null ? department.trim() : "");
        student.setAcademicYear(academicYear != null ? academicYear.trim() : "");

        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));

        // Delete enrollments and attendances
        enrollmentRepository.findByStudentId(id).forEach(enrollmentRepository::delete);
        attendanceRepository.findByStudentIdOrderByMarkedAtDesc(id).forEach(attendanceRepository::delete);

        studentRepository.delete(student);
    }

    @Transactional(readOnly = true)
    public long getTotalStudentsCount() {
        return studentRepository.count();
    }
}
