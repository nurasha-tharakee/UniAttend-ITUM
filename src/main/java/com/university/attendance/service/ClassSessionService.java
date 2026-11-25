package com.university.attendance.service;

import com.university.attendance.dto.SessionCreateRequest;
import com.university.attendance.entity.ClassSession;
import com.university.attendance.entity.Lecturer;
import com.university.attendance.entity.Subject;
import com.university.attendance.repository.ClassSessionRepository;
import com.university.attendance.repository.LecturerRepository;
import com.university.attendance.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final SubjectRepository subjectRepository;
    private final LecturerRepository lecturerRepository;
    private final QrCodeService qrCodeService;
    private final SystemSettingService systemSettingService;

    public ClassSessionService(ClassSessionRepository classSessionRepository,
                               SubjectRepository subjectRepository,
                               LecturerRepository lecturerRepository,
                               QrCodeService qrCodeService,
                               SystemSettingService systemSettingService) {
        this.classSessionRepository = classSessionRepository;
        this.subjectRepository = subjectRepository;
        this.lecturerRepository = lecturerRepository;
        this.qrCodeService = qrCodeService;
        this.systemSettingService = systemSettingService;
    }

    public ClassSession createSession(SessionCreateRequest request, String lecturerUsername) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + request.getSubjectId()));

        Lecturer lecturer = lecturerRepository.findByUserUsername(lecturerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with username: " + lecturerUsername));

        ClassSession session = new ClassSession(
                subject,
                lecturer,
                request.getSessionName().trim(),
                request.getSessionDate(),
                request.getStartTime()
        );

        // Generate dynamic QR token expiring in 2 minutes
        session.setQrToken(generateToken());
        session.setQrTokenExpiresAt(LocalDateTime.now().plusMinutes(2).withNano(0));
        session.setActive(true);

        return classSessionRepository.save(session);
    }

    public ClassSession refreshQrCode(Long sessionId, String lecturerUsername) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        if (!session.getLecturer().getUser().getUsername().equals(lecturerUsername)) {
            throw new SecurityException("You are not authorized to refresh the QR code for this session.");
        }

        if (!session.isActive()) {
            throw new IllegalStateException("Cannot refresh QR code for an inactive session.");
        }

        session.setQrToken(generateToken());
        session.setQrTokenExpiresAt(LocalDateTime.now().plusMinutes(2).withNano(0));

        return classSessionRepository.save(session);
    }

    public ClassSession endSession(Long sessionId, String lecturerUsername) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        if (!session.getLecturer().getUser().getUsername().equals(lecturerUsername)) {
            throw new SecurityException("You are not authorized to end this session.");
        }

        session.setActive(false);
        session.setEndTime(LocalTime.now());
        session.setQrTokenExpiresAt(LocalDateTime.now()); // expire immediately

        return classSessionRepository.save(session);
    }

    public ClassSession getOrRefreshActiveSession(Long sessionId, String lecturerUsername) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        if (!session.getLecturer().getUser().getUsername().equals(lecturerUsername)) {
            throw new SecurityException("You are not authorized to view this session.");
        }

        // If session is active and QR token is expired or missing, auto-refresh it
        if (session.isActive() && (session.isQrExpired() || session.getQrToken() == null)) {
            session.setQrToken(generateToken());
            session.setQrTokenExpiresAt(LocalDateTime.now().plusMinutes(2).withNano(0));
            session = classSessionRepository.save(session);
        }

        return session;
    }

    public String generateQrImageBase64(String qrToken) {
        String baseUrl = systemSettingService.getServerBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String scanUrl = baseUrl + "/student/scan?token=" + qrToken;
        return qrCodeService.generateQrCodeBase64(scanUrl, 320, 320);
    }

    public String getQrFullUrl(String qrToken) {
        String baseUrl = systemSettingService.getServerBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/student/scan?token=" + qrToken;
    }

    @Transactional(readOnly = true)
    public Optional<ClassSession> getSessionById(Long sessionId) {
        return classSessionRepository.findById(sessionId);
    }

    @Transactional(readOnly = true)
    public Optional<ClassSession> getSessionByQrToken(String qrToken) {
        return classSessionRepository.findByQrToken(qrToken);
    }

    @Transactional(readOnly = true)
    public List<ClassSession> getSessionsByLecturerUsername(String username) {
        return classSessionRepository.findByLecturerUserUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<ClassSession> getSessionsBySubject(Long subjectId) {
        return classSessionRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId);
    }

    @Transactional(readOnly = true)
    public List<ClassSession> getAllSessions() {
        return classSessionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getTotalSessionsCount() {
        return classSessionRepository.count();
    }

    private String generateToken() {
        return "ATT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
