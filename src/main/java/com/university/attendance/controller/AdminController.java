package com.university.attendance.controller;

import com.university.attendance.dto.LecturerRegisterRequest;
import com.university.attendance.dto.ReportFilterDto;
import com.university.attendance.dto.StudentRegisterRequest;
import com.university.attendance.dto.SubjectDto;
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

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StudentService studentService;
    private final LecturerService lecturerService;
    private final SubjectService subjectService;
    private final EnrollmentService enrollmentService;
    private final ClassSessionService classSessionService;
    private final AttendanceService attendanceService;
    private final SystemSettingService systemSettingService;
    private final AbsenceRequestService absenceRequestService;
    private final AuditLogService auditLogService;
    private final AcademicStructureService academicStructureService;
    private final ReportExportService reportExportService;

    public AdminController(StudentService studentService,
                           LecturerService lecturerService,
                           SubjectService subjectService,
                           EnrollmentService enrollmentService,
                           ClassSessionService classSessionService,
                           AttendanceService attendanceService,
                           SystemSettingService systemSettingService,
                           AbsenceRequestService absenceRequestService,
                           AuditLogService auditLogService,
                           AcademicStructureService academicStructureService,
                           ReportExportService reportExportService) {
        this.studentService = studentService;
        this.lecturerService = lecturerService;
        this.subjectService = subjectService;
        this.enrollmentService = enrollmentService;
        this.classSessionService = classSessionService;
        this.attendanceService = attendanceService;
        this.systemSettingService = systemSettingService;
        this.absenceRequestService = absenceRequestService;
        this.auditLogService = auditLogService;
        this.academicStructureService = academicStructureService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalStudents = studentService.getTotalStudentsCount();
        long totalLecturers = lecturerService.getTotalLecturersCount();
        long totalSubjects = subjectService.getTotalSubjectsCount();
        long totalSessions = classSessionService.getTotalSessionsCount();
        long totalAttendance = attendanceService.getTotalAttendanceCount();
        long pendingAbsences = absenceRequestService.countPendingTotal();
        long pendingRegistrations = studentService.countPendingRegistrations();
        List<ClassSession> allSessions = classSessionService.getAllSessions();
        long activeSessionsCount = allSessions.stream().filter(ClassSession::isActive).count();

        List<NdtDiscipline> disciplines = academicStructureService.getAllDisciplines();
        List<Student> allStudents = studentService.getAllStudents();
        List<Subject> allSubjects = subjectService.getAllSubjects();

        java.util.Map<Long, Long> disciplineStudentCounts = new java.util.HashMap<>();
        java.util.Map<Long, Long> disciplineSubjectCounts = new java.util.HashMap<>();
        for (NdtDiscipline d : disciplines) {
            long sc = allStudents.stream()
                    .filter(s -> (s.getDiscipline() != null && s.getDiscipline().getId().equals(d.getId()))
                            || (s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(d.getName())))
                    .count();
            long subC = allSubjects.stream()
                    .filter(sub -> sub.getDisciplines().stream().anyMatch(sd -> sd.getId().equals(d.getId()))
                            || (sub.getDepartment() != null && sub.getDepartment().toLowerCase().contains(d.getName().toLowerCase())))
                    .count();
            disciplineStudentCounts.put(d.getId(), sc);
            disciplineSubjectCounts.put(d.getId(), subC);
        }

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalLecturers", totalLecturers);
        model.addAttribute("totalSubjects", totalSubjects);
        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("activeSessionsCount", activeSessionsCount);
        model.addAttribute("totalAttendance", totalAttendance);
        model.addAttribute("pendingAbsences", pendingAbsences);
        model.addAttribute("pendingRegistrations", pendingRegistrations);
        model.addAttribute("disciplines", disciplines);
        model.addAttribute("disciplineStudentCounts", disciplineStudentCounts);
        model.addAttribute("disciplineSubjectCounts", disciplineSubjectCounts);

        // Recent sessions & recent attendance
        model.addAttribute("recentSessions", allSessions.stream().limit(5).toList());
        model.addAttribute("recentAttendance", attendanceService.getAllAttendance().stream().limit(5).toList());

        return "admin/dashboard";
    }

    // ==========================================
    // Student Management & Verifications
    // ==========================================
    @GetMapping("/students")
    public String studentsPage(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "discipline", required = false) String discipline,
                               @RequestParam(value = "status", required = false) String status,
                               Model model) {
        List<Student> students = studentService.filterStudents(search, discipline, status);
        model.addAttribute("students", students);
        model.addAttribute("search", search);
        model.addAttribute("selectedDiscipline", discipline);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("disciplines", academicStructureService.getActiveDisciplines());
        model.addAttribute("academicYears", academicStructureService.getActiveAcademicYears());
        model.addAttribute("batches", academicStructureService.getActiveBatches());
        model.addAttribute("yearsOfStudy", AcademicStructureService.NDT_YEARS_OF_STUDY);
        model.addAttribute("allSemesters", AcademicStructureService.ALL_NDT_SEMESTERS);
        model.addAttribute("pendingRegistrationsCount", studentService.countPendingRegistrations());
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new StudentRegisterRequest());
        }
        return "admin/students";
    }

    @PostMapping("/students/{id}/edit")
    public String editStudent(@PathVariable("id") Long id,
                              @RequestParam("fullName") String fullName,
                              @RequestParam("email") String email,
                              @RequestParam(value = "contactNumber", required = false) String contactNumber,
                              @RequestParam(value = "disciplineId", required = false) Long disciplineId,
                              @RequestParam(value = "batchIntake", required = false) String batchIntake,
                              @RequestParam(value = "academicYear", required = false) String academicYear,
                              @RequestParam(value = "yearOfStudy", required = false) String yearOfStudy,
                              @RequestParam(value = "semester", required = false) String semester,
                              @RequestParam(value = "studentGroup", required = false) String studentGroup,
                              RedirectAttributes redirectAttributes,
                              Authentication authentication,
                              HttpServletRequest httpRequest) {
        try {
            studentService.updateStudentAdmin(id, fullName, email, contactNumber, disciplineId,
                    batchIntake, academicYear, yearOfStudy, semester, studentGroup);
            auditLogService.log(authentication.getName(), "UPDATE_STUDENT", "Updated student ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Student details updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/{id}/toggle-active")
    public String toggleStudentActive(@PathVariable("id") Long id,
                                      RedirectAttributes redirectAttributes,
                                      Authentication authentication,
                                      HttpServletRequest httpRequest) {
        try {
            studentService.toggleStudentActive(id);
            auditLogService.log(authentication.getName(), "TOGGLE_STUDENT_STATUS", "Toggled active status for student ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Student account status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/student-verifications")
    public String studentVerificationsPage(Model model) {
        model.addAttribute("pendingStudents", studentService.getPendingVerificationStudents());
        model.addAttribute("authorizedRecords", studentService.getAllAuthorizedRecords());
        return "admin/student-verifications";
    }

    @PostMapping("/student-verifications/{id}/approve")
    public String approveStudentRegistration(@PathVariable("id") Long id,
                                             Authentication authentication,
                                             HttpServletRequest httpRequest,
                                             RedirectAttributes redirectAttributes) {
        try {
            Student s = studentService.approveStudent(id, authentication.getName());
            auditLogService.log(authentication.getName(), "APPROVE_STUDENT",
                    "Approved student registration: " + s.getStudentIdNumber(), getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Student '" + s.getFullName() + "' approved and activated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/student-verifications";
    }

    @PostMapping("/student-verifications/{id}/reject")
    public String rejectStudentRegistration(@PathVariable("id") Long id,
                                            @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                                            Authentication authentication,
                                            HttpServletRequest httpRequest,
                                            RedirectAttributes redirectAttributes) {
        try {
            Student s = studentService.rejectStudent(id, rejectionReason, authentication.getName());
            auditLogService.log(authentication.getName(), "REJECT_STUDENT",
                    "Rejected student registration: " + s.getStudentIdNumber(), getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Student registration rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/student-verifications";
    }

    @PostMapping("/students/import-authorized")
    public String importAuthorizedStudents(@RequestParam("csvFile") org.springframework.web.multipart.MultipartFile csvFile,
                                           Authentication authentication,
                                           HttpServletRequest httpRequest,
                                           RedirectAttributes redirectAttributes) {
        try {
            int count = studentService.importAuthorizedStudentsCsv(csvFile);
            auditLogService.log(authentication.getName(), "IMPORT_AUTHORIZED_STUDENTS",
                    "Imported " + count + " authorized student records via CSV", getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Imported " + count + " authorized student record(s) successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/student-verifications";
    }

    @PostMapping("/students/register")
    public String registerStudent(@Valid @ModelAttribute("registerRequest") StudentRegisterRequest request,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Authentication authentication,
                                  HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", request);
            redirectAttributes.addFlashAttribute("showRegisterModal", true);
            return "redirect:/admin/students";
        }

        try {
            studentService.registerStudent(request);
            auditLogService.log(authentication.getName(), "REGISTER_STUDENT",
                    "Registered student: " + request.getFullName() + " (" + request.getStudentIdNumber() + ")",
                    getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Student '" + request.getFullName() + "' registered successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", request);
            redirectAttributes.addFlashAttribute("showRegisterModal", true);
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable("id") Long id,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication,
                                HttpServletRequest httpRequest) {
        try {
            studentService.deleteStudent(id);
            auditLogService.log(authentication.getName(), "DELETE_STUDENT", "Deleted student ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    // ==========================================
    // Lecturer Management
    // ==========================================
    @GetMapping("/lecturers")
    public String lecturersPage(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "disciplineId", required = false) Long disciplineId,
                                @RequestParam(value = "divisionId", required = false) Long divisionId,
                                @RequestParam(value = "status", required = false) String status,
                                Model model) {
        List<Lecturer> lecturers = lecturerService.filterLecturers(search, disciplineId, divisionId, status);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("search", search);
        model.addAttribute("selectedDisciplineId", disciplineId);
        model.addAttribute("selectedDivisionId", divisionId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("disciplines", academicStructureService.getActiveDisciplines());
        model.addAttribute("divisions", academicStructureService.getActiveDivisions());
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new LecturerRegisterRequest());
        }
        return "admin/lecturers";
    }

    @PostMapping("/lecturers/{id}/edit")
    public String editLecturer(@PathVariable("id") Long id,
                               @RequestParam(value = "title", required = false) String title,
                               @RequestParam("fullName") String fullName,
                               @RequestParam("email") String email,
                               @RequestParam(value = "contactNumber", required = false) String contactNumber,
                               @RequestParam(value = "divisionId", required = false) Long divisionId,
                               @RequestParam(value = "designation", required = false) String designation,
                               @RequestParam(value = "department", required = false) String department,
                               @RequestParam(value = "disciplineIds", required = false) List<Long> disciplineIds,
                               @RequestParam(value = "active", defaultValue = "false") boolean active,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication,
                               HttpServletRequest httpRequest) {
        try {
            lecturerService.updateLecturerAdmin(id, title, fullName, email, contactNumber,
                    divisionId, designation, department, disciplineIds, active);
            auditLogService.log(authentication.getName(), "UPDATE_LECTURER", "Updated lecturer ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer details updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating lecturer: " + e.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/lecturers/{id}/toggle-active")
    public String toggleLecturerActive(@PathVariable("id") Long id,
                                       RedirectAttributes redirectAttributes,
                                       Authentication authentication,
                                       HttpServletRequest httpRequest) {
        try {
            lecturerService.toggleLecturerActive(id);
            auditLogService.log(authentication.getName(), "TOGGLE_LECTURER_STATUS", "Toggled active status for lecturer ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer account status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/lecturers/register")
    public String registerLecturer(@Valid @ModelAttribute("registerRequest") LecturerRegisterRequest request,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication,
                                   HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", request);
            redirectAttributes.addFlashAttribute("showRegisterModal", true);
            return "redirect:/admin/lecturers";
        }

        try {
            lecturerService.registerLecturer(request);
            auditLogService.log(authentication.getName(), "REGISTER_LECTURER",
                    "Registered lecturer: " + request.getFullName(), getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer '" + request.getFullName() + "' registered successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", request);
            redirectAttributes.addFlashAttribute("showRegisterModal", true);
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/lecturers/{id}/delete")
    public String deleteLecturer(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication,
                                 HttpServletRequest httpRequest) {
        try {
            lecturerService.deleteLecturer(id);
            auditLogService.log(authentication.getName(), "DELETE_LECTURER", "Deleted lecturer ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting lecturer: " + e.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    // ==========================================
    // Subject Management
    // ==========================================
    @GetMapping("/subjects")
    public String subjectsPage(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "disciplineId", required = false) Long disciplineId,
                               @RequestParam(value = "semester", required = false) String semester,
                               @RequestParam(value = "status", required = false) String status,
                               Model model) {
        List<Subject> subjects = subjectService.searchAndFilterSubjects(search, disciplineId, semester, status);
        List<Lecturer> lecturers = lecturerService.getAllLecturers();

        model.addAttribute("subjects", subjects);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("search", search);
        model.addAttribute("selectedDisciplineId", disciplineId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("disciplines", academicStructureService.getActiveDisciplines());
        model.addAttribute("academicYears", academicStructureService.getActiveAcademicYears());
        model.addAttribute("batches", academicStructureService.getActiveBatches());
        model.addAttribute("allSemesters", AcademicStructureService.ALL_NDT_SEMESTERS);
        if (!model.containsAttribute("subjectDto")) {
            model.addAttribute("subjectDto", new SubjectDto());
        }
        return "admin/subjects";
    }

    @GetMapping("/subjects/{id}")
    public String viewSubjectDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Subject subject = subjectService.getSubjectById(id).orElse(null);
        if (subject == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Subject not found.");
            return "redirect:/admin/subjects";
        }
        model.addAttribute("subject", subject);
        model.addAttribute("enrollments", enrollmentService.getEnrollmentsBySubject(id));
        model.addAttribute("sessions", classSessionService.getSessionsBySubject(id));
        model.addAttribute("lecturers", lecturerService.getAllLecturers());
        model.addAttribute("disciplines", academicStructureService.getActiveDisciplines());
        model.addAttribute("academicYears", academicStructureService.getActiveAcademicYears());
        model.addAttribute("batches", academicStructureService.getActiveBatches());
        model.addAttribute("allSemesters", AcademicStructureService.ALL_NDT_SEMESTERS);
        return "admin/subject-detail";
    }

    @PostMapping({"/subjects/add", "/subjects/create"})
    public String createSubject(@Valid @ModelAttribute("subjectDto") SubjectDto dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication,
                                HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subjectDto", bindingResult);
            redirectAttributes.addFlashAttribute("subjectDto", dto);
            redirectAttributes.addFlashAttribute("showAddModal", true);
            return "redirect:/admin/subjects";
        }

        try {
            subjectService.createSubject(dto);
            auditLogService.log(authentication.getName(), "CREATE_SUBJECT",
                    "Created subject: " + dto.getCode() + " - " + dto.getName(), getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Subject '" + dto.getCode() + "' created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("subjectDto", dto);
            redirectAttributes.addFlashAttribute("showAddModal", true);
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/edit")
    public String editSubject(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("subjectDto") SubjectDto dto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Authentication authentication,
                              HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid subject details. Please check code and name.");
            return "redirect:/admin/subjects";
        }
        try {
            subjectService.updateSubject(id, dto);
            auditLogService.log(authentication.getName(), "UPDATE_SUBJECT",
                    "Updated subject ID " + id + " (" + dto.getCode() + ")", getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Subject '" + dto.getCode() + "' updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating subject: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/archive")
    public String archiveSubject(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication,
                                 HttpServletRequest httpRequest) {
        try {
            Subject sub = subjectService.archiveSubject(id);
            auditLogService.log(authentication.getName(), "ARCHIVE_SUBJECT",
                    "Changed subject " + sub.getCode() + " status to " + sub.getStatus(), getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subject '" + sub.getCode() + "' status updated to " + sub.getStatus().getDisplayName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/assign-lecturer")
    public String assignLecturer(@PathVariable("id") Long subjectId,
                                 @RequestParam(value = "lecturerId", required = false) Long lecturerId,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication,
                                 HttpServletRequest httpRequest) {
        try {
            subjectService.assignLecturer(subjectId, lecturerId);
            auditLogService.log(authentication.getName(), "ASSIGN_LECTURER",
                    "Assigned lecturer " + lecturerId + " to subject " + subjectId, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer assigned to subject successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning lecturer: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/delete")
    public String deleteSubject(@PathVariable("id") Long id,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication,
                                HttpServletRequest httpRequest) {
        try {
            subjectService.deleteSubject(id);
            auditLogService.log(authentication.getName(), "DELETE_SUBJECT", "Deleted subject ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting subject: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    // ==========================================
    // Enrollment Management
    // ==========================================
    @GetMapping("/enrollments")
    public String enrollmentsPage(@RequestParam(value = "subjectId", required = false) Long subjectId, Model model) {
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("selectedSubjectId", subjectId);

        if (subjectId != null) {
            model.addAttribute("enrollments", enrollmentService.getEnrollmentsBySubject(subjectId));
        } else {
            List<Subject> subjects = subjectService.getAllSubjects();
            if (!subjects.isEmpty()) {
                Long firstSubjectId = subjects.get(0).getId();
                model.addAttribute("selectedSubjectId", firstSubjectId);
                model.addAttribute("enrollments", enrollmentService.getEnrollmentsBySubject(firstSubjectId));
            } else {
                model.addAttribute("enrollments", List.of());
            }
        }

        return "admin/enrollments";
    }

    @PostMapping("/enrollments/add")
    public String addEnrollment(@RequestParam("subjectId") Long subjectId,
                                @RequestParam("studentIds") List<Long> studentIds,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication,
                                HttpServletRequest httpRequest) {
        try {
            enrollmentService.enrollMultipleStudents(studentIds, subjectId);
            auditLogService.log(authentication.getName(), "ENROLL_STUDENTS",
                    "Enrolled " + studentIds.size() + " students in subject " + subjectId, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Selected students enrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error enrolling students: " + e.getMessage());
        }
        return "redirect:/admin/enrollments?subjectId=" + subjectId;
    }

    @PostMapping("/enrollments/{id}/delete")
    public String deleteEnrollment(@PathVariable("id") Long id,
                                   @RequestParam(value = "subjectId", required = false) Long subjectId,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication,
                                   HttpServletRequest httpRequest) {
        try {
            enrollmentService.removeEnrollment(id);
            auditLogService.log(authentication.getName(), "REMOVE_ENROLLMENT", "Removed enrollment ID: " + id, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Enrollment removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error removing enrollment: " + e.getMessage());
        }
        return "redirect:/admin/enrollments" + (subjectId != null ? "?subjectId=" + subjectId : "");
    }

    // ==========================================
    // Lecture Sessions Management
    // ==========================================
    @GetMapping("/sessions")
    public String sessionsPage(Model model) {
        List<ClassSession> sessions = classSessionService.getAllSessions();
        model.addAttribute("sessions", sessions);
        return "admin/sessions";
    }

    @PostMapping("/sessions/{id}/end")
    public String endSessionAdmin(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication,
                                 HttpServletRequest httpRequest) {
        try {
            ClassSession session = classSessionService.getSessionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
            session.setActive(false);
            session.setQrTokenExpiresAt(java.time.LocalDateTime.now());
            auditLogService.log(authentication.getName(), "ADMIN_END_SESSION",
                    "Admin ended session #" + id + " (" + session.getSessionName() + ")", getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Session marked as ended.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sessions";
    }

    // ==========================================
    // Academic Structure (Divisions, Disciplines, Years, Batches)
    // ==========================================
    @GetMapping("/academic-structure")
    public String academicStructurePage(Model model) {
        model.addAttribute("divisions", academicStructureService.getAllDivisions());
        model.addAttribute("disciplines", academicStructureService.getAllDisciplines());
        model.addAttribute("academicYears", academicStructureService.getAllAcademicYears());
        model.addAttribute("batches", academicStructureService.getAllBatches());
        model.addAttribute("yearsOfStudy", AcademicStructureService.NDT_YEARS_OF_STUDY);
        model.addAttribute("allSemesters", AcademicStructureService.ALL_NDT_SEMESTERS);
        return "admin/academic-structure";
    }

    @PostMapping("/academic-structure/divisions/add")
    public String addDivision(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes redirectAttributes,
                              Authentication authentication,
                              HttpServletRequest httpRequest) {
        try {
            academicStructureService.saveDivision(name, description);
            auditLogService.log(authentication.getName(), "ADD_DIVISION", "Added academic division: " + name, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Academic division added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/academic-structure";
    }

    @PostMapping("/academic-structure/disciplines/{id}/update")
    public String updateDisciplineConfig(@PathVariable("id") Long id,
                                         @RequestParam(value = "code", required = false) String code,
                                         @RequestParam(value = "studentIdFormat", required = false) String studentIdFormat,
                                         @RequestParam(value = "divisionId", required = false) Long divisionId,
                                         @RequestParam(value = "active", defaultValue = "false") boolean active,
                                         RedirectAttributes redirectAttributes,
                                         Authentication authentication,
                                         HttpServletRequest httpRequest) {
        try {
            NdtDiscipline d = academicStructureService.updateDisciplineConfig(id, code, studentIdFormat, divisionId, active);
            auditLogService.log(authentication.getName(), "UPDATE_DISCIPLINE", "Updated config for " + d.getName(), getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Updated configuration for " + d.getName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/academic-structure";
    }

    @PostMapping("/academic-structure/years/add")
    public String addAcademicYear(@RequestParam("name") String name,
                                  @RequestParam(value = "currentYear", defaultValue = "false") boolean currentYear,
                                  RedirectAttributes redirectAttributes,
                                  Authentication authentication,
                                  HttpServletRequest httpRequest) {
        try {
            academicStructureService.saveAcademicYear(name, currentYear);
            auditLogService.log(authentication.getName(), "ADD_ACADEMIC_YEAR", "Added academic year: " + name, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Academic year '" + name + "' added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/academic-structure";
    }

    @PostMapping("/academic-structure/years/{id}/set-current")
    public String setCurrentAcademicYear(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            academicStructureService.setCurrentAcademicYear(id);
            redirectAttributes.addFlashAttribute("successMessage", "Current academic year updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/academic-structure";
    }

    @PostMapping("/academic-structure/batches/add")
    public String addBatchIntake(@RequestParam("name") String name,
                                 @RequestParam(value = "academicYear", required = false) String academicYear,
                                 @RequestParam(value = "studentGroup", required = false) String studentGroup,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication,
                                 HttpServletRequest httpRequest) {
        try {
            academicStructureService.saveBatchIntake(name, academicYear, studentGroup);
            auditLogService.log(authentication.getName(), "ADD_BATCH", "Added batch/intake: " + name, getClientIp(httpRequest));
            redirectAttributes.addFlashAttribute("successMessage", "Batch / Intake '" + name + "' added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/academic-structure";
    }

    @PostMapping("/academic-structure/batches/{id}/toggle")
    public String toggleBatchStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            academicStructureService.toggleBatchStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Batch status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/academic-structure";
    }

    // ==========================================
    // Reports & Exports
    // ==========================================
    @GetMapping("/reports")
    public String reportsPage(@ModelAttribute("filter") ReportFilterDto filter, Model model) {
        List<NdtDiscipline> disciplines = academicStructureService.getAllDisciplines();
        List<Attendance> reports = attendanceService.filterAttendance(filter);

        java.util.Map<String, Long> disciplineAttendanceCounts = new java.util.LinkedHashMap<>();
        for (NdtDiscipline d : disciplines) {
            long cnt = reports.stream()
                    .filter(a -> a.getStudent().getDepartment() != null
                            && a.getStudent().getDepartment().equalsIgnoreCase(d.getName()))
                    .count();
            disciplineAttendanceCounts.put(d.getName(), cnt);
        }

        model.addAttribute("disciplines", disciplines);
        model.addAttribute("lecturers", lecturerService.getAllLecturers());
        model.addAttribute("academicYears", academicStructureService.getAllAcademicYears());
        model.addAttribute("batches", academicStructureService.getAllBatches());
        model.addAttribute("allSemesters", AcademicStructureService.ALL_NDT_SEMESTERS);
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("reports", reports);
        model.addAttribute("totalFiltered", reports.size());
        model.addAttribute("disciplineAttendanceCounts", disciplineAttendanceCounts);

        return "admin/reports";
    }

    @GetMapping("/reports/export/csv")
    public ResponseEntity<byte[]> exportReportsCsv(@ModelAttribute ReportFilterDto filter) {
        List<Attendance> reports = attendanceService.filterAttendance(filter);
        byte[] csv = reportExportService.exportAttendanceCsv(reports);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"uniattend-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportReportsPdf(@ModelAttribute ReportFilterDto filter) {
        List<Attendance> reports = attendanceService.filterAttendance(filter);
        String summary = (filter.getDiscipline() != null && !filter.getDiscipline().isBlank() ? filter.getDiscipline() : "All Disciplines");
        byte[] pdf = reportExportService.exportAttendancePdf(reports, summary);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"uniattend-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ==========================================
    // Absence Requests Management (Admin)
    // ==========================================
    @GetMapping("/absence-requests")
    public String absenceRequestsPage(@RequestParam(value = "status", required = false) String statusFilter,
                                      Model model) {
        List<AbsenceRequest> all = absenceRequestService.getAllRequests();
        List<AbsenceRequest> filtered = all.stream()
                .filter(r -> statusFilter == null || statusFilter.isEmpty() || r.getStatus().name().equalsIgnoreCase(statusFilter))
                .toList();

        model.addAttribute("requests", filtered);
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("totalCount", all.size());
        model.addAttribute("pendingCount", all.stream().filter(r -> r.getStatus() == AbsenceRequestStatus.PENDING).count());
        model.addAttribute("approvedCount", all.stream().filter(r -> r.getStatus() == AbsenceRequestStatus.APPROVED).count());

        return "admin/absence-requests";
    }

    @PostMapping("/absence-requests/{id}/review")
    public String reviewAbsenceRequestAdmin(@PathVariable("id") Long id,
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
        return "redirect:/admin/absence-requests";
    }

    @GetMapping("/absence-requests/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachmentAdmin(@PathVariable("id") Long id,
                                                            Authentication authentication) {
        try {
            Resource resource = absenceRequestService.loadAttachmentAsResource(id, authentication.getName(), true, false);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==========================================
    // GPS Campus Settings (Admin)
    // ==========================================
    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("campusName", systemSettingService.getSettingValue(SystemSettingService.KEY_CAMPUS_NAME, "Institute of Technology, University of Moratuwa (ITUM)"));
        model.addAttribute("campusLatitude", systemSettingService.getCampusLatitude());
        model.addAttribute("campusLongitude", systemSettingService.getCampusLongitude());
        model.addAttribute("allowedRadius", systemSettingService.getAllowedRadiusMeters());
        model.addAttribute("gpsEnforcement", systemSettingService.isGpsEnforcementEnabled());
        model.addAttribute("serverBaseUrl", systemSettingService.getServerBaseUrl());
        model.addAttribute("absenceDocMandatory", systemSettingService.isAbsenceDocMandatory());
        return "admin/settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam("campusName") String campusName,
                               @RequestParam("campusLatitude") Double campusLatitude,
                               @RequestParam("campusLongitude") Double campusLongitude,
                               @RequestParam("allowedRadius") Double allowedRadius,
                               @RequestParam(value = "gpsEnforcement", defaultValue = "false") boolean gpsEnforcement,
                               @RequestParam("serverBaseUrl") String serverBaseUrl,
                               @RequestParam(value = "absenceDocMandatory", defaultValue = "false") boolean absenceDocMandatory,
                               Authentication authentication,
                               HttpServletRequest httpRequest,
                               RedirectAttributes redirectAttributes) {
        try {
            systemSettingService.setSettingValue(SystemSettingService.KEY_CAMPUS_NAME, campusName, "Official Campus Name");
            systemSettingService.setSettingValue(SystemSettingService.KEY_CAMPUS_LATITUDE, String.valueOf(campusLatitude), "Campus Latitude");
            systemSettingService.setSettingValue(SystemSettingService.KEY_CAMPUS_LONGITUDE, String.valueOf(campusLongitude), "Campus Longitude");
            systemSettingService.setSettingValue(SystemSettingService.KEY_ALLOWED_RADIUS, String.valueOf(allowedRadius.intValue()), "Allowed Radius in Meters");
            systemSettingService.setSettingValue(SystemSettingService.KEY_GPS_ENFORCEMENT, String.valueOf(gpsEnforcement), "Enforce GPS Geofence");
            systemSettingService.setSettingValue(SystemSettingService.KEY_SERVER_BASE_URL, serverBaseUrl.trim(), "Server Base LAN URL");
            systemSettingService.setSettingValue(SystemSettingService.KEY_ABSENCE_DOC_MANDATORY, String.valueOf(absenceDocMandatory), "Absence Document Mandatory");

            auditLogService.log(authentication.getName(), "UPDATE_SETTINGS",
                    "Updated campus settings (GPS enforcement=" + gpsEnforcement + ", BaseUrl=" + serverBaseUrl + ")",
                    getClientIp(httpRequest));

            redirectAttributes.addFlashAttribute("successMessage", "System & GPS settings updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving settings: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    // ==========================================
    // Audit Logs
    // ==========================================
    @GetMapping("/audit-logs")
    public String auditLogsPage(Model model) {
        List<AuditLog> logs = auditLogService.getRecentLogs();
        model.addAttribute("logs", logs);
        return "admin/audit-logs";
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            remoteAddr = request.getRemoteAddr();
        }
        return remoteAddr != null ? remoteAddr : "Unknown";
    }
}
