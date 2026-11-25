package com.university.attendance;

import com.university.attendance.dto.AttendanceResponse;
import com.university.attendance.dto.SessionCreateRequest;
import com.university.attendance.entity.ClassSession;
import com.university.attendance.entity.Subject;
import com.university.attendance.service.AttendanceService;
import com.university.attendance.service.ClassSessionService;
import com.university.attendance.service.SubjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class AttendanceApplicationTests {

    @Autowired
    private ClassSessionService classSessionService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private SubjectService subjectService;

    @Test
    @DisplayName("End-to-end QR Attendance flow: Create session, scan QR, duplicate prevention, expiration")
    void testCompleteAttendanceLifecycle() {
        // 1. Fetch seeded subject (CS101)
        List<Subject> subjects = subjectService.getAllSubjects();
        assertFalse(subjects.isEmpty(), "Subjects should be seeded");
        Subject cs101 = subjects.stream().filter(s -> s.getCode().equals("CS101")).findFirst().orElseThrow();

        // 2. Lecturer (dr.smith) creates an attendance session
        SessionCreateRequest request = new SessionCreateRequest();
        request.setSubjectId(cs101.getId());
        request.setSessionName("Unit Test: Graph Traversal Algorithms");
        request.setSessionDate(LocalDate.now());
        request.setStartTime(LocalTime.now());

        ClassSession session = classSessionService.createSession(request, "dr.smith");
        assertNotNull(session.getId());
        assertNotNull(session.getQrToken());
        assertTrue(session.getQrToken().startsWith("ATT-"));
        assertFalse(session.isQrExpired(), "New session QR token should not be expired");

        // 3. QR image generation
        String qrImageBase64 = classSessionService.generateQrImageBase64(session.getQrToken());
        assertNotNull(qrImageBase64);
        assertTrue(qrImageBase64.startsWith("data:image/png;base64,"));

        // 4. Student (student1, who is enrolled in CS101) marks attendance within ITUM Diyagama campus
        double itumLat = 6.80894;
        double itumLon = 79.99282;

        AttendanceResponse response = attendanceService.markAttendance(
                session.getQrToken(),
                "student1",
                "127.0.0.1",
                "JUnit Integration Test Browser",
                itumLat,
                itumLon
        );
        assertTrue(response.isSuccess(), "Enrolled student within ITUM campus should successfully mark attendance: " + response.getMessage());
        assertEquals("CS101", response.getSubjectCode());

        // 4b. Test GPS Geofence Rejection: Student marks from outside ITUM campus (Colombo Fort)
        AttendanceResponse outsideGpsResponse = attendanceService.markAttendance(
                session.getQrToken(),
                "student2",
                "127.0.0.1",
                "JUnit Remote Browser",
                6.9344, // Colombo Fort
                79.8428
        );
        assertFalse(outsideGpsResponse.isSuccess(), "Attendance outside ITUM campus radius must be rejected");
        assertTrue(outsideGpsResponse.getMessage().contains("GPS Verification Failed") || outsideGpsResponse.getMessage().contains("ITUM campus"));

        // 5. Duplicate attendance prevention: student1 tries to mark again for same session
        AttendanceResponse duplicateResponse = attendanceService.markAttendance(
                session.getQrToken(),
                "student1",
                "127.0.0.1",
                "JUnit Integration Test Browser",
                itumLat,
                itumLon
        );
        assertFalse(duplicateResponse.isSuccess(), "Duplicate attendance must be blocked");
        assertTrue(duplicateResponse.getMessage().contains("already marked"));

        // 6. Unenrolled student check: student3 is not enrolled in CS101
        AttendanceResponse unenrolledResponse = attendanceService.markAttendance(
                session.getQrToken(),
                "student3",
                "127.0.0.1",
                "JUnit Integration Test Browser",
                itumLat,
                itumLon
        );
        assertFalse(unenrolledResponse.isSuccess(), "Unenrolled student must be rejected");
        assertTrue(unenrolledResponse.getMessage().contains("not enrolled"));

        // 7. QR Expiration check: refresh token, then simulate expiration
        session = classSessionService.refreshQrCode(session.getId(), "dr.smith");
        assertNotNull(session.getQrToken());

        // End session
        classSessionService.endSession(session.getId(), "dr.smith");
        AttendanceResponse endedResponse = attendanceService.markAttendance(
                session.getQrToken(),
                "student2",
                "127.0.0.1",
                "JUnit Browser",
                itumLat,
                itumLon
        );
        assertFalse(endedResponse.isSuccess(), "Ended session must reject attendance");
    }
}
