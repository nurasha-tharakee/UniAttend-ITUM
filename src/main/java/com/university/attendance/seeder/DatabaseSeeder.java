package com.university.attendance.seeder;

import com.university.attendance.entity.*;
import com.university.attendance.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository,
                          StudentRepository studentRepository,
                          LecturerRepository lecturerRepository,
                          SubjectRepository subjectRepository,
                          EnrollmentRepository enrollmentRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        System.out.println(">>> Initializing default university attendance data...");

        // 1. Seed Admin
        User adminUser = new User("admin", passwordEncoder.encode("admin123"), Role.ROLE_ADMIN);
        userRepository.save(adminUser);

        // 2. Seed Lecturers
        User lecUser1 = new User("dr.smith", passwordEncoder.encode("lecturer123"), Role.ROLE_LECTURER);
        userRepository.save(lecUser1);
        Lecturer lec1 = new Lecturer(lecUser1, "EMP-101", "Dr. Alan Smith", "alan.smith@university.edu", "Computer Science");
        lec1 = lecturerRepository.save(lec1);

        User lecUser2 = new User("prof.johnson", passwordEncoder.encode("lecturer123"), Role.ROLE_LECTURER);
        userRepository.save(lecUser2);
        Lecturer lec2 = new Lecturer(lecUser2, "EMP-102", "Prof. Sarah Johnson", "sarah.johnson@university.edu", "Software Engineering");
        lec2 = lecturerRepository.save(lec2);

        // 3. Seed Students
        User stuUser1 = new User("student1", passwordEncoder.encode("student123"), Role.ROLE_STUDENT);
        userRepository.save(stuUser1);
        Student stu1 = new Student(stuUser1, "STU-2024-001", "Alex Morgan", "alex.morgan@student.edu", "Computer Science", "Year 2");
        stu1 = studentRepository.save(stu1);

        User stuUser2 = new User("student2", passwordEncoder.encode("student123"), Role.ROLE_STUDENT);
        userRepository.save(stuUser2);
        Student stu2 = new Student(stuUser2, "STU-2024-002", "Emily Davis", "emily.davis@student.edu", "Software Engineering", "Year 2");
        stu2 = studentRepository.save(stu2);

        User stuUser3 = new User("student3", passwordEncoder.encode("student123"), Role.ROLE_STUDENT);
        userRepository.save(stuUser3);
        Student stu3 = new Student(stuUser3, "STU-2024-003", "Michael Brown", "michael.brown@student.edu", "Information Systems", "Year 3");
        stu3 = studentRepository.save(stu3);

        // 4. Seed Subjects
        Subject sub1 = new Subject("CS101", "Data Structures & Algorithms", "Computer Science", "Semester 1", lec1);
        sub1 = subjectRepository.save(sub1);

        Subject sub2 = new Subject("SE201", "Object-Oriented Software Engineering", "Software Engineering", "Semester 1", lec2);
        sub2 = subjectRepository.save(sub2);

        Subject sub3 = new Subject("CS301", "Database Management Systems", "Computer Science", "Semester 2", lec1);
        sub3 = subjectRepository.save(sub3);

        Subject sub4 = new Subject("IS202", "Web Application Architecture", "Information Systems", "Semester 2", lec2);
        sub4 = subjectRepository.save(sub4);

        // 5. Seed Enrollments
        LocalDateTime past = LocalDateTime.now().minusDays(10).withNano(0);

        Enrollment e1 = new Enrollment(stu1, sub1);
        e1.setEnrolledAt(past);
        enrollmentRepository.save(e1);

        Enrollment e2 = new Enrollment(stu1, sub2);
        e2.setEnrolledAt(past);
        enrollmentRepository.save(e2);

        Enrollment e3 = new Enrollment(stu1, sub3);
        e3.setEnrolledAt(past);
        enrollmentRepository.save(e3);

        Enrollment e4 = new Enrollment(stu2, sub1);
        e4.setEnrolledAt(past);
        enrollmentRepository.save(e4);

        Enrollment e5 = new Enrollment(stu2, sub2);
        e5.setEnrolledAt(past);
        enrollmentRepository.save(e5);

        Enrollment e6 = new Enrollment(stu2, sub4);
        e6.setEnrolledAt(past);
        enrollmentRepository.save(e6);

        Enrollment e7 = new Enrollment(stu3, sub3);
        e7.setEnrolledAt(past);
        enrollmentRepository.save(e7);

        Enrollment e8 = new Enrollment(stu3, sub4);
        e8.setEnrolledAt(past);
        enrollmentRepository.save(e8);

        System.out.println(">>> University attendance default data initialized successfully!");
        System.out.println("    Admin: admin / admin123");
        System.out.println("    Lecturers: dr.smith / lecturer123, prof.johnson / lecturer123");
        System.out.println("    Students: student1 / student123, student2 / student123, student3 / student123");
    }
}
