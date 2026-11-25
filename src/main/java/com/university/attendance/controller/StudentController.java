package com.university.attendance.controller;

import com.university.attendance.dto.AbsenceRequestCreateDto;
import com.university.attendance.dto.AttendanceResponse;
import com.university.attendance.dto.MarkAttendanceRequest;
import com.university.attendance.dto.StudentOverallStatsDto;
import com.university.attendance.entity.AbsenceReasonCategory;
import com.university.attendance.entity.AbsenceRequest;
import com.university.attendance.entity.Attendance;
import com.university.attendance.entity.ClassSession;
import com.university.attendance.entity.Enrollment;
import com.university.attendance.entity.Student;
import com.university.attendance.entity.Subject;
import com.university.attendance.repository.EnrollmentRepository;
import com.university.attendance.service.AbsenceRequestService;
import com.university.attendance.service.AttendanceService;
import com.university.attendance.service.ClassSessionService;
import com.university.attendance.service.StudentService;
import com.university.attendance.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final AbsenceRequestService absenceRequestService;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionService classSessionService;
    private final SystemSettingService systemSettingService;

    public StudentController(StudentService studentService,
                             AttendanceService attendanceService,
                             AbsenceRequestService absenceRequestService,
                             EnrollmentRepository enrollmentRepository,
                             ClassSessionService classSessionService,
                             SystemSettingService systemSettingService) {
        this.studentService = studentService;
        this.attendanceService = attendanceService;
        this.absenceRequestService = absenceRequestService;
        this.enrollmentRepository = enrollmentRepository;
        this.classSessionService = classSessionService;
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        // Calculate student's overall & subject-wise stats strictly for this student
        StudentOverallStatsDto stats = attendanceService.getStudentOverallStats(username);
        List<Attendance> recentAttendance = attendanceService.getAttendanceHistoryForStudent(username);
        List<AbsenceRequest> recentRequests = absenceRequestService.getRequestsForStudent(username);

        model.addAttribute("student", student);
        model.addAttribute("stats", stats);
        model.addAttribute("recentAttendance", recentAttendance.stream().limit(5).toList());
        model.addAttribute("recentRequests", recentRequests.stream().limit(3).toList());
        model.addAttribute("totalRequestsCount", recentRequests.size());
        model.addAttribute("pendingRequestsCount", recentRequests.stream()
                .filter(r -> r.getStatus().name().equals("PENDING")).count());

        return "student/dashboard";
    }

    @GetMapping("/scan")
    public String scanPage(@RequestParam(value = "token", required = false) String token,
                           Authentication authentication, Model model) {
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        model.addAttribute("student", student);
        model.addAttribute("prefilledToken", token != null ? token.trim() : "");
        model.addAttribute("gpsEnforcement", systemSettingService.isGpsEnforcementEnabled());
        return "student/scan";
    }

    @PostMapping("/mark-attendance")
    @ResponseBody
    public ResponseEntity<AttendanceResponse> markAttendance(@RequestBody MarkAttendanceRequest request,
                                                             Authentication authentication,
                                                             HttpServletRequest httpRequest) {
        String username = authentication.getName();
        String ipAddress = getClientIp(httpRequest);
        String deviceInfo = request.getDeviceInfo() != null && !request.getDeviceInfo().isEmpty()
                ? request.getDeviceInfo()
                : httpRequest.getHeader("User-Agent");

        AttendanceResponse response = attendanceService.markAttendance(
                request.getQrToken(),
                username,
                ipAddress,
                deviceInfo,
                request.getLatitude(),
                request.getLongitude()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/history")
    public String history(Authentication authentication, Model model) {
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        List<Attendance> attendanceHistory = attendanceService.getAttendanceHistoryForStudent(username);
        StudentOverallStatsDto stats = attendanceService.getStudentOverallStats(username);

        model.addAttribute("student", student);
        model.addAttribute("attendanceHistory", attendanceHistory);
        model.addAttribute("stats", stats);

        return "student/history";
    }

    // ==========================================
    // Absence Requests Module (Phases 2 & 3)
    // ==========================================
    @GetMapping("/absence-requests")
    public String listAbsenceRequests(Authentication authentication, Model model) {
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        List<AbsenceRequest> requests = absenceRequestService.getRequestsForStudent(username);
        model.addAttribute("student", student);
        model.addAttribute("requests", requests);

        return "student/absence-requests";
    }

    @GetMapping("/absence-requests/new")
    public String newAbsenceRequestForm(Authentication authentication, Model model) {
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        List<Subject> subjects = enrollments.stream().map(Enrollment::getSubject).toList();
        List<ClassSession> recentSessions = classSessionService.getAllSessions().stream().limit(20).toList();

        model.addAttribute("student", student);
        model.addAttribute("subjects", subjects);
        model.addAttribute("sessions", recentSessions);
        model.addAttribute("categories", AbsenceReasonCategory.values());
        model.addAttribute("isDocMandatory", systemSettingService.isAbsenceDocMandatory());

        if (!model.containsAttribute("requestDto")) {
            model.addAttribute("requestDto", new AbsenceRequestCreateDto());
        }

        return "student/submit-absence";
    }

    @PostMapping("/absence-requests")
    public String submitAbsenceRequest(@ModelAttribute("requestDto") AbsenceRequestCreateDto dto,
                                       Authentication authentication,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Student student = studentService.getStudentByUsername(username).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }

        try {
            absenceRequestService.submitRequest(student, dto, getClientIp(request));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Absence request submitted successfully! Your lecturer will review it shortly.");
            return "redirect:/student/absence-requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("requestDto", dto);
            return "redirect:/student/absence-requests/new";
        }
    }

    @GetMapping("/absence-requests/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id,
                                                       Authentication authentication) {
        try {
            Resource resource = absenceRequestService.loadAttachmentAsResource(id, authentication.getName(), false, false);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        Student student = studentService.getStudentByUsername(authentication.getName()).orElse(null);
        if (student == null) {
            return "redirect:/login";
        }
        model.addAttribute("student", student);
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(value = "contactNumber", required = false) String contactNumber,
                                @RequestParam(value = "personalEmail", required = false) String personalEmail,
                                @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            studentService.updateStudentProfile(authentication.getName(), contactNumber, personalEmail, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Your profile has been updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/profile";
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            remoteAddr = request.getRemoteAddr();
        }
        return remoteAddr != null ? remoteAddr : "Unknown";
    }
}
