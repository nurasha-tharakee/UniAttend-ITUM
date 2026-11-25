package com.university.attendance.controller;

import com.university.attendance.dto.StudentRegisterRequest;
import com.university.attendance.entity.Student;
import com.university.attendance.service.AcademicStructureService;
import com.university.attendance.service.AuditLogService;
import com.university.attendance.service.StudentService;
import com.university.attendance.service.VerificationRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final StudentService studentService;
    private final AcademicStructureService academicStructureService;
    private final VerificationRateLimiterService rateLimiter;
    private final AuditLogService auditLogService;

    public AuthController(StudentService studentService,
                          AcademicStructureService academicStructureService,
                          VerificationRateLimiterService rateLimiter,
                          AuditLogService auditLogService) {
        this.studentService = studentService;
        this.academicStructureService = academicStructureService;
        this.rateLimiter = rateLimiter;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    return "redirect:/admin/dashboard";
                } else if ("ROLE_LECTURER".equals(role)) {
                    return "redirect:/lecturer/dashboard";
                } else if ("ROLE_STUDENT".equals(role)) {
                    return "redirect:/student/dashboard";
                }
            }
        }
        return "welcome";
    }

    @GetMapping("/welcome")
    public String welcomePage(Authentication authentication, Model model) {
        model.addAttribute("isAuthenticated", authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal()));
        return "welcome";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Authentication authentication,
                            Model model) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password, or your account is awaiting administrator approval.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new StudentRegisterRequest());
        }
        populateRegistrationOptions(model);
        return "register";
    }

    @PostMapping("/register")
    public String handleSelfRegistration(@Valid @ModelAttribute("registerRequest") StudentRegisterRequest request,
                                         BindingResult bindingResult,
                                         HttpServletRequest httpRequest,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        String ip = getClientIp(httpRequest);
        if (!rateLimiter.allowAttempt("REG:" + ip)) {
            model.addAttribute("errorMessage", "Too many registration attempts from your IP. Please wait 10 minutes and try again.");
            populateRegistrationOptions(model);
            return "register";
        }

        if (bindingResult.hasErrors()) {
            populateRegistrationOptions(model);
            return "register";
        }

        try {
            Student saved = studentService.selfRegisterStudent(request);
            auditLogService.log(saved.getUser().getUsername(), "SELF_REGISTER_STUDENT",
                    "Student self-registered: " + saved.getStudentIdNumber() + " (IdentityMatched=" + saved.isIdentityVerified() + ")", ip);

            redirectAttributes.addFlashAttribute("demoOtp", saved.getVerificationOtp());
            redirectAttributes.addFlashAttribute("identityMatched", saved.isIdentityVerified());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration submitted! Please verify your university email ownership below before administrator approval.");
            return "redirect:/verify-email?studentId=" + saved.getStudentIdNumber();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateRegistrationOptions(model);
            return "register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmailPage(@RequestParam(value = "studentId", required = false) String studentId, Model model) {
        model.addAttribute("studentId", studentId != null ? studentId : "");
        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String handleVerifyEmail(@RequestParam("studentIdNumber") String studentIdNumber,
                                    @RequestParam("otp") String otp,
                                    HttpServletRequest httpRequest,
                                    RedirectAttributes redirectAttributes) {
        String ip = getClientIp(httpRequest);
        if (!rateLimiter.allowAttempt("OTP:" + ip + ":" + studentIdNumber)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Too many verification attempts. Please wait 10 minutes and try again.");
            return "redirect:/verify-email?studentId=" + studentIdNumber;
        }

        try {
            Student verified = studentService.verifyEmailOtp(studentIdNumber, otp);
            auditLogService.log(verified.getUser().getUsername(), "VERIFY_STUDENT_EMAIL",
                    "Verified email OTP for Student ID: " + verified.getStudentIdNumber(), ip);
            redirectAttributes.addFlashAttribute("verifiedComplete", true);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Email verified! Your registration (" + verified.getStudentIdNumber()
                            + ") is now Pending Approval by an ITUM administrator.");
            return "redirect:/verify-email?studentId=" + verified.getStudentIdNumber();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/verify-email?studentId=" + studentIdNumber;
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private void populateRegistrationOptions(Model model) {
        model.addAttribute("disciplines", academicStructureService.getActiveDisciplines());
        model.addAttribute("academicYears", academicStructureService.getActiveAcademicYears());
        model.addAttribute("batches", academicStructureService.getActiveBatches());
        model.addAttribute("yearsOfStudy", AcademicStructureService.NDT_YEARS_OF_STUDY);
        model.addAttribute("yearToSemesters", academicStructureService.getYearToSemestersMap());
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : request.getRemoteAddr();
    }
}
