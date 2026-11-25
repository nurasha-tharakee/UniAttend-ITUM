package com.university.attendance.service;

import com.university.attendance.dto.SubjectDto;
import com.university.attendance.entity.Lecturer;
import com.university.attendance.entity.NdtDiscipline;
import com.university.attendance.entity.Subject;
import com.university.attendance.entity.SubjectStatus;
import com.university.attendance.repository.ClassSessionRepository;
import com.university.attendance.repository.EnrollmentRepository;
import com.university.attendance.repository.LecturerRepository;
import com.university.attendance.repository.NdtDisciplineRepository;
import com.university.attendance.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final LecturerRepository lecturerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final NdtDisciplineRepository disciplineRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          LecturerRepository lecturerRepository,
                          EnrollmentRepository enrollmentRepository,
                          ClassSessionRepository classSessionRepository,
                          NdtDisciplineRepository disciplineRepository) {
        this.subjectRepository = subjectRepository;
        this.lecturerRepository = lecturerRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classSessionRepository = classSessionRepository;
        this.disciplineRepository = disciplineRepository;
    }

    public Subject createSubject(SubjectDto dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (subjectRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Subject code '" + code + "' already exists.");
        }

        Lecturer lecturer = null;
        if (dto.getLecturerId() != null) {
            lecturer = lecturerRepository.findById(dto.getLecturerId())
                    .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with ID: " + dto.getLecturerId()));
        }

        Set<NdtDiscipline> disciplines = resolveDisciplines(dto.getDisciplineIds());
        String deptDisplay = resolveDepartmentDisplay(dto.getDepartment(), disciplines, dto.isCommonSubject());

        Subject subject = new Subject(
                code,
                dto.getName().trim(),
                deptDisplay,
                dto.getSemester() != null ? dto.getSemester().trim() : "",
                lecturer
        );
        subject.setDisciplines(disciplines);
        subject.setCommonSubject(dto.isCommonSubject() || disciplines.size() > 1);
        subject.setAcademicYear(dto.getAcademicYear() != null ? dto.getAcademicYear().trim() : "");
        subject.setBatchIntake(dto.getBatchIntake() != null ? dto.getBatchIntake().trim() : "");
        subject.setCreditValue(dto.getCreditValue());
        subject.setStatus(parseStatus(dto.getStatus()));

        return subjectRepository.save(subject);
    }

    public Subject updateSubject(Long id, SubjectDto dto) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + id));

        String newCode = dto.getCode().trim().toUpperCase();
        if (!subject.getCode().equalsIgnoreCase(newCode) && subjectRepository.existsByCode(newCode)) {
            throw new IllegalArgumentException("Subject code '" + newCode + "' already exists.");
        }

        Set<NdtDiscipline> disciplines = resolveDisciplines(dto.getDisciplineIds());
        String deptDisplay = resolveDepartmentDisplay(dto.getDepartment(), disciplines, dto.isCommonSubject());

        subject.setCode(newCode);
        subject.setName(dto.getName().trim());
        subject.setDepartment(deptDisplay);
        subject.setDisciplines(disciplines);
        subject.setCommonSubject(dto.isCommonSubject() || disciplines.size() > 1);
        subject.setAcademicYear(dto.getAcademicYear() != null ? dto.getAcademicYear().trim() : "");
        subject.setSemester(dto.getSemester() != null ? dto.getSemester().trim() : "");
        subject.setBatchIntake(dto.getBatchIntake() != null ? dto.getBatchIntake().trim() : "");
        subject.setCreditValue(dto.getCreditValue());
        subject.setStatus(parseStatus(dto.getStatus()));

        if (dto.getLecturerId() != null) {
            Lecturer lecturer = lecturerRepository.findById(dto.getLecturerId())
                    .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with ID: " + dto.getLecturerId()));
            subject.setLecturer(lecturer);
        } else {
            subject.setLecturer(null);
        }

        return subjectRepository.save(subject);
    }

    private Set<NdtDiscipline> resolveDisciplines(List<Long> disciplineIds) {
        if (disciplineIds == null || disciplineIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(disciplineRepository.findAllById(disciplineIds));
    }

    private String resolveDepartmentDisplay(String fallbackDept, Set<NdtDiscipline> disciplines, boolean commonSubject) {
        if (disciplines != null && !disciplines.isEmpty()) {
            if (disciplines.size() == 1 && !commonSubject) {
                return disciplines.iterator().next().getName();
            }
            return disciplines.stream().map(NdtDiscipline::getName).sorted()
                    .reduce((a, b) -> a + ", " + b).orElse("Common / Interdisciplinary");
        }
        return fallbackDept != null ? fallbackDept.trim() : "";
    }

    private SubjectStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return SubjectStatus.ACTIVE;
        }
        try {
            return SubjectStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SubjectStatus.ACTIVE;
        }
    }

    public Subject archiveSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + id));
        subject.setStatus(subject.getStatus() == SubjectStatus.ARCHIVED ? SubjectStatus.ACTIVE : SubjectStatus.ARCHIVED);
        return subjectRepository.save(subject);
    }

    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + id));

        if (!classSessionRepository.findBySubjectIdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete subject '" + subject.getCode()
                    + "' because it has existing lecture sessions or attendance records. Please archive it instead.");
        }

        enrollmentRepository.findBySubjectId(id).forEach(enrollmentRepository::delete);
        subjectRepository.delete(subject);
    }

    @Transactional(readOnly = true)
    public List<Subject> searchAndFilterSubjects(String search, Long disciplineId, String semester, String status) {
        return getAllSubjects().stream()
                .filter(s -> search == null || search.isBlank()
                        || s.getCode().toLowerCase().contains(search.trim().toLowerCase())
                        || s.getName().toLowerCase().contains(search.trim().toLowerCase())
                        || (s.getDepartment() != null && s.getDepartment().toLowerCase().contains(search.trim().toLowerCase())))
                .filter(s -> disciplineId == null
                        || s.getDisciplines().stream().anyMatch(d -> d.getId().equals(disciplineId))
                        || (s.getDepartment() != null && disciplineRepository.findById(disciplineId)
                            .map(d -> s.getDepartment().toLowerCase().contains(d.getName().toLowerCase())).orElse(false)))
                .filter(s -> semester == null || semester.isBlank() || semester.equalsIgnoreCase(s.getSemester()))
                .filter(s -> status == null || status.isBlank() || s.getStatus().name().equalsIgnoreCase(status))
                .toList();
    }

    public Subject assignLecturer(Long subjectId, Long lecturerId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found with ID: " + subjectId));

        if (lecturerId == null || lecturerId == 0) {
            subject.setLecturer(null);
        } else {
            Lecturer lecturer = lecturerRepository.findById(lecturerId)
                    .orElseThrow(() -> new IllegalArgumentException("Lecturer not found with ID: " + lecturerId));
            subject.setLecturer(lecturer);
        }

        return subjectRepository.save(subject);
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Subject> getSubjectById(Long id) {
        return subjectRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByLecturerId(Long lecturerId) {
        return subjectRepository.findByLecturerId(lecturerId);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByLecturerUsername(String username) {
        return subjectRepository.findByLecturerUserUsername(username);
    }

    @Transactional(readOnly = true)
    public long getTotalSubjectsCount() {
        return subjectRepository.count();
    }
}
