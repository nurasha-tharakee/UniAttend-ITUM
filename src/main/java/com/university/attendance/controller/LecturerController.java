package com.university.attendance.controller;

import com.university.attendance.dto.SessionCreateRequest;
import com.university.attendance.entity.*;
import com.university.attendance.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private final LecturerService lecturerService;
    private final SubjectService subjectService;
    private final EnrollmentService enrollmentService;
    private final ClassSessionService classSessionService;
    private final AttendanceService attendanceService;
    private final AbsenceRequestService absenceRequestService;

    public LecturerController(LecturerService lecturerService,
                              SubjectService subjectService,
                              EnrollmentService enrollmentService,
                              ClassSessionService classSessionService,
                              AttendanceService attendanceService,
                              AbsenceRequestService absenceRequestService) {
        this.lecturerService = lecturerService;
        this.subjectService = subjectService;
        this.enrollmentService = enrollmentService;
        this.classSessionService = classSessionService;
        this.attendanceService = attendanceService;
        this.absenceRequestService = absenceRequestService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        Lecturer lecturer = lecturerService.getLecturerByUsername(username).orElse(null);
        if (lecturer == null) {
            return "redirect:/login";
        }

        List<Subject> subjects = subjectService.getSubjectsByLecturerUsername(username);
        List<ClassSession> recentSessions = classSessionService.getSessionsByLecturerUsername(username);

        // Calculate students count map per subject
        Map<Long, Long> subjectStudentCounts = new HashMap<>();
        long totalStudents = 0;
        for (Subject s : subjects) {
            long count = enrollmentService.getEnrolledStudentsCount(s.getId());
            subjectStudentCounts.put(s.getId(), count);
            totalStudents += count;
        }

        long pendingAbsenceRequests = absenceRequestService.countPendingForLecturer(username);

        model.addAttribute("lecturer", lecturer);
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectStudentCounts", subjectStudentCounts);
        model.addAttribute("totalAssignedSubjects", subjects.size());
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("recentSessions", recentSessions.stream().limit(5).toList());
        model.addAttribute("activeSessionsCount", recentSessions.stream().filter(ClassSession::isActive).count());
        model.addAttribute("pendingAbsenceRequests", pendingAbsenceRequests);

        if (!model.containsAttribute("sessionRequest")) {
            model.addAttribute("sessionRequest", new SessionCreateRequest());
        }

        return "lecturer/dashboard";
    }

    @GetMapping("/sessions")
    public String sessions(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<ClassSession> sessions = classSessionService.getSessionsByLecturerUsername(username);
        List<Subject> subjects = subjectService.getSubjectsByLecturerUsername(username);

        model.addAttribute("sessions", sessions);
        model.addAttribute("subjects", subjects);
        if (!model.containsAttribute("sessionRequest")) {
            model.addAttribute("sessionRequest", new SessionCreateRequest());
        }
        return "lecturer/sessions";
    }

    @PostMapping("/sessions/create")
    public String createSession(@Valid @ModelAttribute("sessionRequest") SessionCreateRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.sessionRequest", bindingResult);
            redirectAttributes.addFlashAttribute("sessionRequest", request);
            return "redirect:/lecturer/dashboard";
        }

        try {
            ClassSession session = classSessionService.createSession(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Class session started successfully!");
            return "redirect:/lecturer/sessions/" + session.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("sessionRequest", request);
            return "redirect:/lecturer/dashboard";
        }
    }

    @GetMapping("/sessions/{id}")
    public String viewSession(@PathVariable("id") Long id, Authentication authentication, Model model) {
        String username = authentication.getName();
        // Use getOrRefreshActiveSession to ensure an active session ALWAYS has a valid 2-minute scannable QR code!
        ClassSession session = classSessionService.getOrRefreshActiveSession(id, username);

        // Generate Base64 QR code image
        String qrImage = null;
        long remainingSeconds = 0;
        if (session.isActive() && !session.isQrExpired() && session.getQrToken() != null) {
            qrImage = classSessionService.generateQrImageBase64(session.getQrToken());
            remainingSeconds = Math.max(0,
                    Duration.between(LocalDateTime.now(), session.getQrTokenExpiresAt()).getSeconds());
        }

        List<Attendance> attendances = attendanceService.getAttendanceForSession(id);
        long totalEnrolled = enrollmentService.getEnrolledStudentsCount(session.getSubject().getId());

        model.addAttribute("session", session);
        model.addAttribute("classSession", session);
        model.addAttribute("qrImage", qrImage);
        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("attendances", attendances);
        model.addAttribute("totalEnrolled", totalEnrolled);
        model.addAttribute("attendanceCount", attendances.size());

        return "lecturer/session-detail";
    }

    @PostMapping("/sessions/{id}/refresh-qr")
    public String refreshQr(@PathVariable("id") Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            classSessionService.refreshQrCode(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "QR Code refreshed! Valid for 2 minutes.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error refreshing QR Code: " + e.getMessage());
        }
        return "redirect:/lecturer/sessions/" + id;
    }

    // Dynamic QR Auto-Refresh API endpoint for zero-reload client timer
    @GetMapping("/sessions/{id}/auto-refresh-qr")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> autoRefreshQr(@PathVariable("id") Long id,
                                                            Authentication authentication) {
        ClassSession session = classSessionService.getSessionById(id).orElse(null);
        if (session == null || !session.getLecturer().getUser().getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("active", session.isActive());

        if (!session.isActive()) {
            response.put("qrImage", null);
            response.put("remainingSeconds", 0);
            response.put("status", "ENDED");
            return ResponseEntity.ok(response);
        }

        // Regenerate fresh token if expired or close to expiry
        ClassSession refreshed = classSessionService.refreshQrCode(id, authentication.getName());
        String qrImage = classSessionService.generateQrImageBase64(refreshed.getQrToken());
        long remaining = Math.max(0, Duration.between(LocalDateTime.now(), refreshed.getQrTokenExpiresAt()).getSeconds());

        response.put("qrImage", qrImage);
        response.put("remainingSeconds", remaining);
        response.put("token", refreshed.getQrToken());
        response.put("fullUrl", classSessionService.getQrFullUrl(refreshed.getQrToken()));
        response.put("status", "ACTIVE");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{id}/end")
    public String endSession(@PathVariable("id") Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            classSessionService.endSession(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Class session ended successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error ending session: " + e.getMessage());
        }
        return "redirect:/lecturer/sessions/" + id;
    }

    // JSON API for live updating attendance list on session page
    @GetMapping("/sessions/{id}/attendance-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLiveAttendanceData(@PathVariable("id") Long id,
                                                                    Authentication authentication) {
        ClassSession session = classSessionService.getSessionById(id).orElse(null);
        if (session == null || !session.getLecturer().getUser().getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        List<Attendance> attendances = attendanceService.getAttendanceForSession(id);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm:ss a");

        List<Map<String, Object>> list = new ArrayList<>();
        for (Attendance a : attendances) {
            Map<String, Object> item = new HashMap<>();
            item.put("studentIdNumber", a.getStudent().getStudentIdNumber());
            item.put("fullName", a.getStudent().getFullName());
            item.put("department", a.getStudent().getDepartment());
            item.put("markedAt", a.getMarkedAt().format(timeFmt));
            item.put("status", a.getStatus());
            list.add(item);
        }

        long remainingSeconds = 0;
        if (session.isActive() && !session.isQrExpired() && session.getQrTokenExpiresAt() != null) {
            remainingSeconds = Math.max(0,
                    Duration.between(LocalDateTime.now(), session.getQrTokenExpiresAt()).getSeconds());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("active", session.isActive());
        response.put("qrExpired", session.isQrExpired());
        response.put("qrToken", session.getQrToken());
        response.put("remainingSeconds", remainingSeconds);
        response.put("count", attendances.size());
        response.put("attendees", list);

        return ResponseEntity.ok(response);
    }

    // Lecturer Subject Attendance Reports
    @GetMapping("/reports")
    public String reports(@RequestParam(value = "subjectId", required = false) Long subjectId,
                          Authentication authentication,
                          Model model) {
        String username = authentication.getName();
        List<Subject> subjects = subjectService.getSubjectsByLecturerUsername(username);

        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubjectId", subjectId);

        if (subjectId != null) {
            boolean owns = subjects.stream().anyMatch(s -> s.getId().equals(subjectId));
            if (owns) {
                List<Attendance> attendances = attendanceService.getAttendanceForSubject(subjectId);
                model.addAttribute("attendances", attendances);
                model.addAttribute("totalAttendance", attendances.size());
            } else {
                model.addAttribute("attendances", List.of());
            }
        } else if (!subjects.isEmpty()) {
            Long firstId = subjects.get(0).getId();
            model.addAttribute("selectedSubjectId", firstId);
            List<Attendance> attendances = attendanceService.getAttendanceForSubject(firstId);
            model.addAttribute("attendances", attendances);
            model.addAttribute("totalAttendance", attendances.size());
        } else {
            model.addAttribute("attendances", List.of());
        }

        return "lecturer/reports";
    }

    // ==========================================
    // Medical & Absence Requests (Phase 4)
    // ==========================================
    @GetMapping("/absence-requests")
    public String listAbsenceRequests(@RequestParam(value = "subjectId", required = false) Long subjectId,
                                      @RequestParam(value = "status", required = false) String statusFilter,
                                      Authentication authentication,
                                      Model model) {
        String username = authentication.getName();
        List<Subject> subjects = subjectService.getSubjectsByLecturerUsername(username);
        List<AbsenceRequest> allRequests = absenceRequestService.getRequestsForLecturer(username);

        List<AbsenceRequest> filtered = allRequests.stream()
                .filter(r -> subjectId == null || r.getSubject().getId().equals(subjectId))
                .filter(r -> statusFilter == null || statusFilter.isEmpty() || r.getStatus().name().equalsIgnoreCase(statusFilter))
                .toList();

        model.addAttribute("subjects", subjects);
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("requests", filtered);
        model.addAttribute("totalPending", allRequests.stream()
                .filter(r -> r.getStatus() == AbsenceRequestStatus.PENDING).count());

        return "lecturer/absence-requests";
    }

    @PostMapping("/absence-requests/{id}/review")
    public String reviewAbsenceRequest(@PathVariable("id") Long id,
                                      @RequestParam("status") AbsenceRequestStatus status,
                                      @RequestParam(value = "comments", required = false) String comments,
                                      Authentication authentication,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        try {
            absenceRequestService.reviewRequest(id, status, comments, authentication.getName(), getClientIp(request));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Absence request #" + id + " updated to " + status.name() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error reviewing request: " + e.getMessage());
        }
        return "redirect:/lecturer/absence-requests";
    }

    @GetMapping("/absence-requests/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id,
                                                       Authentication authentication) {
        try {
            Resource resource = absenceRequestService.loadAttachmentAsResource(id, authentication.getName(), false, true);
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
        Lecturer lecturer = lecturerService.getLecturerByUsername(authentication.getName()).orElse(null);
        if (lecturer == null) {
            return "redirect:/login";
        }
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("subjects", subjectService.getSubjectsByLecturerUsername(authentication.getName()));
        return "lecturer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(value = "title", required = false) String title,
                                @RequestParam("fullName") String fullName,
                                @RequestParam(value = "contactNumber", required = false) String contactNumber,
                                @RequestParam("email") String email,
                                @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            lecturerService.updateLecturerProfile(authentication.getName(), title, fullName, contactNumber, email, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lecturer/profile";
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            remoteAddr = request.getRemoteAddr();
        }
        return remoteAddr != null ? remoteAddr : "Unknown";
    }
}
