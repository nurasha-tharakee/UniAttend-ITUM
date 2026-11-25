package com.university.attendance.service;

import com.university.attendance.entity.*;
import com.university.attendance.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AcademicStructureService {

    public static final List<String> OFFICIAL_NDT_DISCIPLINES = List.of(
            "Chemical Engineering Technology",
            "Civil Engineering Technology",
            "Electrical Engineering Technology",
            "Electronic and Telecommunication Engineering Technology",
            "Marine Engineering Technology",
            "Mechanical Engineering Technology",
            "Nautical Studies",
            "Polymer Technology",
            "Textile and Clothing Technology",
            "Information Technology"
    );

    public static final List<String> NDT_YEARS_OF_STUDY = List.of(
            "First Year",
            "Second Year",
            "Third Year"
    );

    public static final List<String> ALL_NDT_SEMESTERS = List.of(
            "Semester 1",
            "Semester 2",
            "Semester 3",
            "Semester 4",
            "Semester 5",
            "Semester 6 (Industrial Training)"
    );

    private final AcademicDivisionRepository divisionRepository;
    private final NdtDisciplineRepository disciplineRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BatchIntakeRepository batchIntakeRepository;

    public AcademicStructureService(AcademicDivisionRepository divisionRepository,
                                    NdtDisciplineRepository disciplineRepository,
                                    AcademicYearRepository academicYearRepository,
                                    BatchIntakeRepository batchIntakeRepository) {
        this.divisionRepository = divisionRepository;
        this.disciplineRepository = disciplineRepository;
        this.academicYearRepository = academicYearRepository;
        this.batchIntakeRepository = batchIntakeRepository;
    }

    @PostConstruct
    public void initOfficialStructure() {
        // 1. Seed Academic Divisions (some divisions manage multiple NDT disciplines)
        AcademicDivision chemPolyDiv = ensureDivision("Division of Chemical & Polymer Engineering Technology",
                "Manages Chemical Engineering Technology and Polymer Technology");
        AcademicDivision civilDiv = ensureDivision("Division of Civil Engineering Technology",
                "Manages Civil Engineering Technology");
        AcademicDivision elecDiv = ensureDivision("Division of Electrical & Electronic Engineering Technology",
                "Manages Electrical Engineering Technology and Electronic and Telecommunication Engineering Technology");
        AcademicDivision marineDiv = ensureDivision("Division of Marine & Nautical Studies",
                "Manages Marine Engineering Technology and Nautical Studies");
        AcademicDivision mechDiv = ensureDivision("Division of Mechanical Engineering Technology",
                "Manages Mechanical Engineering Technology");
        AcademicDivision textileDiv = ensureDivision("Division of Textile & Clothing Technology",
                "Manages Textile and Clothing Technology");
        AcademicDivision itDiv = ensureDivision("Division of Information Technology",
                "Manages Information Technology");
        ensureDivision("Division of Interdisciplinary Studies",
                "Manages common foundational subjects across NDT disciplines");

        // 2. Seed all 10 official ITUM NDT Disciplines (idempotent, preserves admin-configured codes/formats)
        ensureDiscipline("Chemical Engineering Technology", chemPolyDiv, 1);
        ensureDiscipline("Civil Engineering Technology", civilDiv, 2);
        ensureDiscipline("Electrical Engineering Technology", elecDiv, 3);
        ensureDiscipline("Electronic and Telecommunication Engineering Technology", elecDiv, 4);
        ensureDiscipline("Marine Engineering Technology", marineDiv, 5);
        ensureDiscipline("Mechanical Engineering Technology", mechDiv, 6);
        ensureDiscipline("Nautical Studies", marineDiv, 7);
        ensureDiscipline("Polymer Technology", chemPolyDiv, 8);
        ensureDiscipline("Textile and Clothing Technology", textileDiv, 9);
        ensureDiscipline("Information Technology", itDiv, 10);

        // 3. Seed default Academic Years if empty
        if (academicYearRepository.count() == 0) {
            academicYearRepository.save(new AcademicYear("2024/2025", true));
            academicYearRepository.save(new AcademicYear("2025/2026", false));
        }

        // 4. Seed default Batches / Intakes if empty
        if (batchIntakeRepository.count() == 0) {
            batchIntakeRepository.save(new BatchIntake("2024 Intake", "2024/2025", "Group A"));
            batchIntakeRepository.save(new BatchIntake("2025 Intake", "2025/2026", "Group A"));
        }
    }

    private AcademicDivision ensureDivision(String name, String description) {
        return divisionRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> divisionRepository.save(new AcademicDivision(name, description)));
    }

    private void ensureDiscipline(String name, AcademicDivision division, int order) {
        if (!disciplineRepository.existsByNameIgnoreCase(name)) {
            disciplineRepository.save(new NdtDiscipline(name, division, order));
        }
    }

    public Map<String, List<String>> getYearToSemestersMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("First Year", List.of("Semester 1", "Semester 2"));
        map.put("Second Year", List.of("Semester 3", "Semester 4"));
        map.put("Third Year", List.of("Semester 5", "Semester 6 (Industrial Training)"));
        return map;
    }

    @Transactional(readOnly = true)
    public List<NdtDiscipline> getAllDisciplines() {
        return disciplineRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<NdtDiscipline> getActiveDisciplines() {
        return disciplineRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<AcademicDivision> getAllDivisions() {
        return divisionRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<AcademicDivision> getActiveDivisions() {
        return divisionRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<AcademicYear> getAllAcademicYears() {
        return academicYearRepository.findAllByOrderByNameDesc();
    }

    @Transactional(readOnly = true)
    public List<AcademicYear> getActiveAcademicYears() {
        return academicYearRepository.findByActiveTrueOrderByNameDesc();
    }

    @Transactional(readOnly = true)
    public List<BatchIntake> getAllBatches() {
        return batchIntakeRepository.findAllByOrderByNameDesc();
    }

    @Transactional(readOnly = true)
    public List<BatchIntake> getActiveBatches() {
        return batchIntakeRepository.findByActiveTrueOrderByNameDesc();
    }

    public AcademicDivision saveDivision(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Division name is required.");
        }
        if (divisionRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("Academic division '" + name.trim() + "' already exists.");
        }
        return divisionRepository.save(new AcademicDivision(name.trim(), description != null ? description.trim() : ""));
    }

    public NdtDiscipline updateDisciplineConfig(Long id, String code, String studentIdFormat, Long divisionId, boolean active) {
        NdtDiscipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discipline not found."));
        discipline.setCode(code != null && !code.trim().isEmpty() ? code.trim() : null);
        discipline.setStudentIdFormat(studentIdFormat != null && !studentIdFormat.trim().isEmpty() ? studentIdFormat.trim() : null);
        if (divisionId != null) {
            AcademicDivision div = divisionRepository.findById(divisionId).orElse(null);
            discipline.setDivision(div);
        }
        discipline.setActive(active);
        return disciplineRepository.save(discipline);
    }

    public AcademicYear saveAcademicYear(String name, boolean currentYear) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Academic year name is required.");
        }
        if (academicYearRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("Academic year '" + name.trim() + "' already exists.");
        }
        if (currentYear) {
            academicYearRepository.findAll().forEach(y -> {
                y.setCurrentYear(false);
                academicYearRepository.save(y);
            });
        }
        return academicYearRepository.save(new AcademicYear(name.trim(), currentYear));
    }

    public void setCurrentAcademicYear(Long id) {
        AcademicYear target = academicYearRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found."));
        academicYearRepository.findAll().forEach(y -> {
            y.setCurrentYear(y.getId().equals(id));
            academicYearRepository.save(y);
        });
    }

    public BatchIntake saveBatchIntake(String name, String academicYear, String studentGroup) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch / Intake name is required.");
        }
        if (batchIntakeRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("Batch / Intake '" + name.trim() + "' already exists.");
        }
        return batchIntakeRepository.save(new BatchIntake(
                name.trim(),
                academicYear != null ? academicYear.trim() : "",
                studentGroup != null ? studentGroup.trim() : ""
        ));
    }

    public void toggleBatchStatus(Long id) {
        BatchIntake batch = batchIntakeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found."));
        batch.setActive(!batch.isActive());
        batchIntakeRepository.save(batch);
    }
}
