package com.university.attendance.service;

import com.university.attendance.dto.AbsenceRequestCreateDto;
import com.university.attendance.entity.*;
import com.university.attendance.repository.AbsenceRequestRepository;
import com.university.attendance.repository.ClassSessionRepository;
import com.university.attendance.repository.SubjectRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AbsenceRequestService {

    private static final String UPLOAD_DIR = "./uploads/absence_documents/";
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "jpg", "jpeg", "png");

    private final AbsenceRequestRepository absenceRequestRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSessionRepository sessionRepository;
    private final SystemSettingService settingService;
    private final AuditLogService auditLogService;

    public AbsenceRequestService(AbsenceRequestRepository absenceRequestRepository,
                                 SubjectRepository subjectRepository,
                                 ClassSessionRepository sessionRepository,
                                 SystemSettingService settingService,
                                 AuditLogService auditLogService) {
        this.absenceRequestRepository = absenceRequestRepository;
        this.subjectRepository = subjectRepository;
        this.sessionRepository = sessionRepository;
        this.settingService = settingService;
        this.auditLogService = auditLogService;

        // Ensure upload directory exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload folder: " + e.getMessage(), e);
        }
    }

    @Transactional
    public AbsenceRequest submitRequest(Student student, AbsenceRequestCreateDto dto, String clientIp) throws IOException {
        if (student == null) {
            throw new IllegalArgumentException("Student must be authenticated");
        }
        if (dto.getSubjectId() == null) {
            throw new IllegalArgumentException("Subject is required");
        }
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        ClassSession session = null;
        if (dto.getSessionId() != null) {
            session = sessionRepository.findById(dto.getSessionId()).orElse(null);
        }

        if (dto.getReasonCategory() == null) {
            throw new IllegalArgumentException("Absence reason category is required");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Start and End dates are required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than start date");
        }
        if (dto.getExplanation() == null || dto.getExplanation().trim().length() < 5) {
            throw new IllegalArgumentException("Detailed explanation is required (minimum 5 characters)");
        }

        MultipartFile file = dto.getDocument();
        boolean isDocMandatory = settingService.isAbsenceDocMandatory();
        if (isDocMandatory && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("Supporting document is mandatory as per institutional policy");
        }

        AbsenceRequest request = new AbsenceRequest(student, subject, session, dto.getReasonCategory(),
                dto.getStartDate(), dto.getEndDate(), dto.getExplanation().trim());

        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("Invalid file type. Allowed formats: PDF, JPG, PNG");
            }
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                throw new IllegalArgumentException("File size must not exceed 10 MB");
            }

            String savedFileName = UUID.randomUUID() + "." + extension;
            Path targetLocation = Paths.get(UPLOAD_DIR).resolve(savedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            request.setAttachmentPath(savedFileName);
            request.setAttachmentOriginalName(originalFilename);
            request.setAttachmentType(file.getContentType());
        }

        AbsenceRequest saved = absenceRequestRepository.save(request);
        auditLogService.log(student.getUser().getUsername(), "SUBMIT_ABSENCE_REQUEST",
                "Submitted " + dto.getReasonCategory().getDisplayName() + " for subject " + subject.getCode(), clientIp);
        return saved;
    }

    @Transactional
    public AbsenceRequest reviewRequest(Long requestId, AbsenceRequestStatus newStatus, String comments,
                                        String reviewerUsername, String clientIp) {
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Absence request not found"));

        request.setStatus(newStatus);
        request.setReviewComments(comments != null ? comments.trim() : null);
        request.setReviewedBy(reviewerUsername);
        request.setReviewedAt(LocalDateTime.now().withNano(0));

        AbsenceRequest updated = absenceRequestRepository.save(request);
        auditLogService.log(reviewerUsername, "REVIEW_ABSENCE_REQUEST",
                "Marked request #" + requestId + " as " + newStatus.name() + " for student " +
                        request.getStudent().getStudentIdNumber(), clientIp);
        return updated;
    }

    public List<AbsenceRequest> getRequestsForStudent(String username) {
        return absenceRequestRepository.findByStudentUserUsernameOrderByCreatedAtDesc(username);
    }

    public List<AbsenceRequest> getRequestsForLecturer(String lecturerUsername) {
        return absenceRequestRepository.findBySubjectLecturerUserUsernameOrderByCreatedAtDesc(lecturerUsername);
    }

    public List<AbsenceRequest> getAllRequests() {
        return absenceRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<AbsenceRequest> getById(Long id) {
        return absenceRequestRepository.findById(id);
    }

    public Resource loadAttachmentAsResource(Long requestId, String username, boolean isAdmin, boolean isLecturer) {
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!isAdmin) {
            if (isLecturer) {
                // Verify lecturer owns the subject
                Lecturer lecturer = request.getSubject().getLecturer();
                if (lecturer == null || !lecturer.getUser().getUsername().equals(username)) {
                    throw new SecurityException("Unauthorized to access this attachment");
                }
            } else {
                // Must be the student who submitted it
                if (!request.getStudent().getUser().getUsername().equals(username)) {
                    throw new SecurityException("Unauthorized to access this attachment");
                }
            }
        }

        if (request.getAttachmentPath() == null) {
            throw new IllegalArgumentException("No attachment found for this request");
        }

        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(request.getAttachmentPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Attachment file not readable or missing on server");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error loading attachment file: " + e.getMessage(), e);
        }
    }

    public long countPendingForLecturer(String lecturerUsername) {
        return absenceRequestRepository.countBySubjectLecturerUserUsernameAndStatus(lecturerUsername, AbsenceRequestStatus.PENDING);
    }

    public long countPendingTotal() {
        return absenceRequestRepository.countByStatus(AbsenceRequestStatus.PENDING);
    }
}
