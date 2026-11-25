package com.university.attendance;

import com.university.attendance.dto.*;
import com.university.attendance.entity.*;
import com.university.attendance.repository.*;
import com.university.attendance.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ItumSystemIntegrationTest {

    @Autowired private AcademicStructureService academicStructureService;
    @Autowired private SubjectService subjectService;
    @Autowired private LecturerService lecturerService;
    @Autowired private StudentService studentService;
    @Autowired private EnrollmentService enrollmentService;
    @Autowired private ClassSessionService classSessionService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private AbsenceRequestService absenceRequestService;
    @Autowired private ReportExportService reportExportService;
    @Autowired private AuthorizedStudentRecordRepository authorizedRecordRepository;

    @Test
    @DisplayName("All 10 official ITUM NDT disciplines and academic structure are seeded")
    void testTenNdtDisciplinesAndAcademicStructure() {
        List<NdtDiscipline> disciplines = academicStructureService.getAllDisciplines();
        assertEquals(10, disciplines.size(), "All 10 official NDT disciplines must exist");
        for (String expected : AcademicStructureService.OFFICIAL_NDT_DISCIPLINES) {
            assertTrue(disciplines.stream().anyMatch(d -> d.getName().equals(expected)),
                    "Missing official NDT discipline: " + expected);
        }
        assertEquals(3, AcademicStructureService.NDT_YEARS_OF_STUDY.size());
        assertEquals(6, AcademicStructureService.ALL_NDT_SEMESTERS.size());
    }

    @Test
    @DisplayName("Subject creation, editing, duplicate prevention, and unsafe deletion prevention")
    void testSubjectCrudAndSafeDelete() {
        NdtDiscipline itDisc = academicStructureService.getAllDisciplines().stream()
                .filter(d -> d.getName().equals("Information Technology")).findFirst().orElseThrow();

        SubjectDto dto = new SubjectDto();
        dto.setCode("NDT-IT-101");
        dto.setName("Programming Fundamentals");
        dto.setDisciplineIds(List.of(itDisc.getId()));
        dto.setAcademicYear("2024/2025");
        dto.setSemester("Semester 1");
        dto.setCreditValue(3);

        Subject created = subjectService.createSubject(dto);
        assertNotNull(created.getId());
        assertEquals("NDT-IT-101", created.getCode());

        // Duplicate prevention
        assertThrows(IllegalArgumentException.class, () -> subjectService.createSubject(dto));

        // Edit subject
        dto.setName("Structured Programming");
        Subject updated = subjectService.updateSubject(created.getId(), dto);
        assertEquals("Structured Programming", updated.getName());

        // Assign lecturer and create session -> unsafe deletion must be blocked
        Lecturer lec = lecturerService.getAllLecturers().get(0);
        subjectService.assignLecturer(created.getId(), lec.getId());
        SessionCreateRequest sReq = new SessionCreateRequest();
        sReq.setSubjectId(created.getId());
        sReq.setSessionName("Lecture 1");
        sReq.setSessionDate(LocalDate.now());
        sReq.setStartTime(LocalTime.now());
        classSessionService.createSession(sReq, lec.getUser().getUsername());

        assertThrows(IllegalStateException.class, () -> subjectService.deleteSubject(created.getId()));

        // Archiving works safely
        Subject archived = subjectService.archiveSubject(created.getId());
        assertEquals(SubjectStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    @DisplayName("Lecturer multi-discipline editing and Student self-registration + ID verification + Approval")
    void testLecturerEditAndStudentVerificationLifecycle() {
        List<NdtDiscipline> disciplines = academicStructureService.getAllDisciplines();
        Lecturer lec = lecturerService.getAllLecturers().get(0);
        Lecturer edited = lecturerService.updateLecturerAdmin(
                lec.getId(), "Dr.", lec.getFullName(), lec.getEmail(), "0711234567",
                disciplines.get(0).getDivision().getId(), "Senior Lecturer",
                disciplines.get(0).getName(), List.of(disciplines.get(0).getId(), disciplines.get(9).getId()), true
        );
        assertEquals(2, edited.getAssociatedDisciplines().size());
        assertEquals("Senior Lecturer", edited.getDesignation());

        // Seed authorized student record
        AuthorizedStudentRecord auth = new AuthorizedStudentRecord();
        auth.setStudentIdNumber("NDT-DEMO-901");
        auth.setFullName("Kasun Perera");
        auth.setInstitutionalEmail("kasun.p@itum.demo");
        auth.setDiscipline("Information Technology");
        auth.setDemonstrationRecord(true);
        authorizedRecordRepository.save(auth);

        // Self-register student
        StudentRegisterRequest reg = new StudentRegisterRequest();
        reg.setUsername("kasun901");
        reg.setPassword("secret123");
        reg.setConfirmPassword("secret123");
        reg.setStudentIdNumber("NDT-DEMO-901");
        reg.setFullName("Kasun Perera");
        reg.setEmail("kasun.p@itum.demo");
        reg.setDisciplineId(disciplines.get(9).getId());
        reg.setBatchIntake("2024 Intake");
        reg.setAcademicYear("2024/2025");
        reg.setYearOfStudy("First Year");
        reg.setSemester("Semester 1");

        Student registered = studentService.selfRegisterStudent(reg);
        assertTrue(registered.isIdentityVerified());
        assertFalse(registered.getUser().isEnabled());
        assertEquals(RegistrationStatus.PENDING_VERIFICATION, registered.getRegistrationStatus());

        // Duplicate registration blocked
        assertThrows(IllegalArgumentException.class, () -> studentService.selfRegisterStudent(reg));

        // Verify email OTP -> transitions to PENDING_APPROVAL
        Student emailVerified = studentService.verifyEmailOtp("NDT-DEMO-901", registered.getVerificationOtp());
        assertEquals(RegistrationStatus.PENDING_APPROVAL, emailVerified.getRegistrationStatus());
        assertFalse(emailVerified.getUser().isEnabled());

        // Unapproved student cannot mark attendance
        AttendanceResponse blocked = attendanceService.markAttendance("ANY", "kasun901", "127.0.0.1", "Test", 6.80894, 79.99282);
        assertFalse(blocked.isSuccess());

        // Admin approves student
        Student approved = studentService.approveStudent(registered.getId(), "admin");
        assertEquals(RegistrationStatus.APPROVED, approved.getRegistrationStatus());
        assertTrue(approved.getUser().isEnabled());
    }

    @Test
    @DisplayName("Discipline-wise reports and CSV/PDF exports work with actual DB records")
    void testReportsAndExports() {
        ReportFilterDto filter = new ReportFilterDto();
        List<Attendance> records = attendanceService.filterAttendance(filter);
        byte[] csv = reportExportService.exportAttendanceCsv(records);
        byte[] pdf = reportExportService.exportAttendancePdf(records, "All");
        assertTrue(csv.length > 20);
        assertTrue(new String(pdf).startsWith("%PDF-1.4"));
    }
}
