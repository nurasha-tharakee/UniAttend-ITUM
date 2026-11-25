package com.university.attendance.service;

import com.university.attendance.dto.AttendanceResponse;
import com.university.attendance.dto.ReportFilterDto;
import com.university.attendance.dto.StudentOverallStatsDto;
import com.university.attendance.dto.SubjectAttendanceStatsDto;
import com.university.attendance.entity.Attendance;
import com.university.attendance.entity.ClassSession;
import com.university.attendance.entity.Enrollment;
import com.university.attendance.entity.Student;
import com.university.attendance.repository.AttendanceRepository;
import com.university.attendance.repository.ClassSessionRepository;
import com.university.attendance.repository.EnrollmentRepository;
import com.university.attendance.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SystemSettingService systemSettingService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a, dd MMM yyyy");

    public AttendanceService(AttendanceRepository attendanceRepository,
                             ClassSessionRepository classSessionRepository,
                             StudentRepository studentRepository,
                             EnrollmentRepository enrollmentRepository,
                             SystemSettingService systemSettingService,
                             AuditLogService auditLogService) {
        this.attendanceRepository = attendanceRepository;
        this.classSessionRepository = classSessionRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.systemSettingService = systemSettingService;
        this.auditLogService = auditLogService;
    }

    public AttendanceResponse markAttendance(String qrToken, String studentUsername, String ipAddress, String deviceInfo) {
        return markAttendance(qrToken, studentUsername, ipAddress, deviceInfo, null, null);
    }

    public AttendanceResponse markAttendance(String qrToken, String studentUsername, String ipAddress, String deviceInfo,
                                            Double latitude, Double longitude) {
        if (qrToken == null || qrToken.trim().isEmpty()) {
            return new AttendanceResponse(false, "Invalid QR code. Token is empty.");
        }

        Student student = studentRepository.findByUserUsername(studentUsername)
                .orElse(null);
        if (student == null) {
            return new AttendanceResponse(false, "Student account not found for user: " + studentUsername);
        }
        if (student.getRegistrationStatus() != com.university.attendance.entity.RegistrationStatus.APPROVED
                || (student.getUser() != null && !student.getUser().isEnabled())) {
            return new AttendanceResponse(false, "Your student account is not yet approved for attendance. Current status: "
                    + student.getRegistrationStatus().getDisplayName());
        }

        Optional<ClassSession> sessionOpt = classSessionRepository.findByQrToken(qrToken.trim());
        if (sessionOpt.isEmpty()) {
            return new AttendanceResponse(false, "Invalid QR Code. No active session found with this code.");
        }

        ClassSession session = sessionOpt.get();

        if (!session.isActive()) {
            return new AttendanceResponse(false, "This attendance session has ended.");
        }

        // Validate 2-minute expiration
        if (session.isQrExpired()) {
            return new AttendanceResponse(false, "This QR Code has expired (2 minutes validity). Please ask your lecturer to refresh the code.");
        }

        // Verify student is enrolled in the subject
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudentIdAndSubjectId(
                student.getId(),
                session.getSubject().getId()
        );

        if (enrollmentOpt.isEmpty()) {
            return new AttendanceResponse(false, "You are not enrolled in " + session.getSubject().getCode() + " (" + session.getSubject().getName() + "). Attendance cannot be marked.");
        }

        Enrollment enrollment = enrollmentOpt.get();

        // Check if student enrolled after session creation
        if (session.getCreatedAt().isBefore(enrollment.getEnrolledAt())) {
            return new AttendanceResponse(false, "You were enrolled in this subject after this session was conducted.");
        }

        // Prevent duplicate attendance for the same student and session
        if (attendanceRepository.existsBySessionIdAndStudentId(session.getId(), student.getId())) {
            return new AttendanceResponse(false, "You have already marked attendance for this session (" + session.getSessionName() + ").");
        }

        // Server-side Geofence Verification if enabled
        if (systemSettingService.isGpsEnforcementEnabled()) {
            if (latitude == null || longitude == null) {
                return new AttendanceResponse(false, "GPS location is required by university attendance policy. Please enable device location / GPS permissions and try again.");
            }
            double campusLat = systemSettingService.getCampusLatitude();
            double campusLon = systemSettingService.getCampusLongitude();
            double allowedRadius = systemSettingService.getAllowedRadiusMeters();
            double distanceMeters = systemSettingService.calculateDistanceMeters(campusLat, campusLon, latitude, longitude);

            if (distanceMeters > allowedRadius) {
                return new AttendanceResponse(false, String.format(
                        "GPS Verification Failed: You are %.0fm away from ITUM campus (permitted radius: %.0fm). You must be present within campus premises to mark attendance.",
                        distanceMeters, allowedRadius));
            }
        }

        // Save Attendance
        Attendance attendance = new Attendance(session, student, ipAddress, deviceInfo, latitude, longitude);
        attendance = attendanceRepository.save(attendance);

        // Audit Log
        auditLogService.log(studentUsername, "MARK_ATTENDANCE",
                "Marked present for " + session.getSubject().getCode() + " (" + session.getSessionName() + ")", ipAddress);

        // Recalculate percentage for this subject
        SubjectAttendanceStatsDto subjectStats = calculateSubjectStats(student, enrollment);

        return new AttendanceResponse(
                true,
                "Attendance marked successfully for " + session.getSubject().getCode() + "!",
                session.getSubject().getCode(),
                session.getSubject().getName(),
                session.getSessionName(),
                attendance.getMarkedAt().format(TIME_FORMATTER),
                subjectStats.getPercentage()
        );
    }

    @Transactional(readOnly = true)
    public StudentOverallStatsDto getStudentOverallStats(String studentUsername) {
        Student student = studentRepository.findByUserUsername(studentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + studentUsername));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        List<SubjectAttendanceStatsDto> subjectStatsList = new ArrayList<>();

        long totalConductedSum = 0;
        long totalPresentSum = 0;

        for (Enrollment enrollment : enrollments) {
            SubjectAttendanceStatsDto sStats = calculateSubjectStats(student, enrollment);
            subjectStatsList.add(sStats);
            totalConductedSum += sStats.getTotalConducted();
            totalPresentSum += sStats.getPresentCount();
        }

        return new StudentOverallStatsDto(totalConductedSum, totalPresentSum, subjectStatsList);
    }

    @Transactional(readOnly = true)
    public SubjectAttendanceStatsDto calculateSubjectStats(Student student, Enrollment enrollment) {
        LocalDateTime enrolledAt = enrollment.getEnrolledAt();
        Long subjectId = enrollment.getSubject().getId();

        // Only count sessions conducted after a student is enrolled in the relevant subject
        long totalConducted = classSessionRepository.countBySubjectIdAndCreatedAtGreaterThanEqual(subjectId, enrolledAt);
        long presentCount = attendanceRepository.countAttendedSessionsForStudentAndSubject(student.getId(), subjectId, enrolledAt);

        String lecturerName = enrollment.getSubject().getLecturer() != null
                ? enrollment.getSubject().getLecturer().getFullName()
                : "Not Assigned";

        return new SubjectAttendanceStatsDto(
                subjectId,
                enrollment.getSubject().getCode(),
                enrollment.getSubject().getName(),
                lecturerName,
                totalConducted,
                presentCount
        );
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceForSession(Long sessionId) {
        return attendanceRepository.findBySessionIdOrderByMarkedAtAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceHistoryForStudent(String studentUsername) {
        return attendanceRepository.findByStudentUserUsernameOrderByMarkedAtDesc(studentUsername);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceForSubject(Long subjectId) {
        return attendanceRepository.findBySubjectId(subjectId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> filterAttendance(ReportFilterDto filter) {
        List<Attendance> base = attendanceRepository.filterAttendance(
                filter.getSubjectId(),
                filter.getStudentId(),
                filter.getStartDate(),
                filter.getEndDate()
        );
        return base.stream()
                .filter(a -> filter.getDiscipline() == null || filter.getDiscipline().isBlank()
                        || (a.getStudent().getDepartment() != null && a.getStudent().getDepartment().equalsIgnoreCase(filter.getDiscipline().trim())))
                .filter(a -> filter.getBatchIntake() == null || filter.getBatchIntake().isBlank()
                        || (a.getStudent().getBatchIntake() != null && a.getStudent().getBatchIntake().equalsIgnoreCase(filter.getBatchIntake().trim())))
                .filter(a -> filter.getAcademicYear() == null || filter.getAcademicYear().isBlank()
                        || (a.getStudent().getAcademicYear() != null && a.getStudent().getAcademicYear().equalsIgnoreCase(filter.getAcademicYear().trim())))
                .filter(a -> filter.getSemester() == null || filter.getSemester().isBlank()
                        || (a.getSession().getSubject().getSemester() != null && a.getSession().getSubject().getSemester().equalsIgnoreCase(filter.getSemester().trim())))
                .filter(a -> filter.getLecturerId() == null
                        || (a.getSession().getLecturer() != null && a.getSession().getLecturer().getId().equals(filter.getLecturerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAllByOrderByMarkedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getTotalAttendanceCount() {
        return attendanceRepository.count();
    }
}
