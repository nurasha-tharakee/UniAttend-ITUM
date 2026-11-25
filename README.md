# University QR Attendance Management System

A complete, modern, responsive University Attendance Management System built with Java 17, Spring Boot, MySQL, Spring Security, Thymeleaf, and ZXing QR Code Library.

---

## Technology Stack
- **Backend**: Java 17, Spring Boot 3.2.5
- **Database**: MySQL Server 8.0 (with in-memory H2 fallback profile)
- **Persistence**: Spring Data JPA / Hibernate
- **Security**: Spring Security with BCrypt password hashing & role-based access control
- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5, Bootstrap Icons
- **Templating**: Thymeleaf + Thymeleaf Extras Spring Security 6
- **QR Generation**: Google ZXing (Zebra Crossing) QR Code Library
- **QR Scanning**: HTML5-QRCode mobile camera scanner with image file upload fallback
- **Data Analytics**: Chart.js for attendance percentage donut & bar charts

---

## Key Features

### 1. User Roles & Authentication
- **Admin**:
  - Register & manage students and lecturers
  - Manage subjects and courses
  - Assign lecturers to subjects
  - Enroll students in subjects (single or multi-select bulk enroll)
  - View overall attendance analytics and comprehensive reports with multi-criteria filters
- **Lecturer**:
  - View assigned courses and total enrolled students
  - Create attendance sessions with dynamic QR codes
  - Live auto-updating attendance roster (polls every 3 seconds)
  - Regenerate / refresh QR code with 2-minute validity
  - Projector mode (fullscreen high-contrast view for lecture halls)
  - View subject-wise attendance reports
- **Student**:
  - Scan dynamic QR codes using mobile phone camera or file upload
  - Automatic attendance validation on the server
  - View personal overall attendance percentage & progress bars
  - View subject-wise attendance breakdown (Total Conducted, Present, Absent, %)
  - View full attendance history log
  - Strict privacy: students can only access their own information

### 2. QR Attendance Security & Expiration
- QR code is dynamically generated per class session.
- QR token contains a secure cryptographically random token (`ATT-<UUID>`).
- **2-Minute Expiration**: Validated strictly on backend server (`qrTokenExpiresAt = now() + 2 minutes`).
- **Duplicate Prevention**: Database unique constraint `(session_id, student_id)` and service checks ensure only 1 attendance record per student per session.
- **Enrollment Validation**: Rejects scanning if student is not enrolled in the subject.
- **Zero Sensitive Data in QR**: Passwords, student IDs, and personal information are NEVER embedded in QR codes.

### 3. Attendance Percentage Formula
$$\text{Attendance Percentage} = \left(\frac{\text{Present Classes}}{\text{Total Conducted Classes}}\right) \times 100$$
- Calculated separately for each subject and overall across all enrolled subjects.
- Only counts sessions conducted after a student's enrollment date.

---

## Default User Accounts (Pre-Seeded)

| Role | Username | Password | Full Name / Description |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | System Administrator |
| **Lecturer** | `dr.smith` | `lecturer123` | Dr. Alan Smith (Computer Science) |
| **Lecturer** | `prof.johnson` | `lecturer123` | Prof. Sarah Johnson (Software Eng) |
| **Student** | `student1` | `student123` | Alex Morgan (STU-2024-001) |
| **Student** | `student2` | `student123` | Emily Davis (STU-2024-002) |
| **Student** | `student3` | `student123` | Michael Brown (STU-2024-003) |

*(Quick-login buttons are also provided directly on the login page for 1-click test credential entry!)*

---

## Running the Application

### Option A: With MySQL (Recommended)
1. Ensure MySQL Server is running on port 3306.
2. In `src/main/resources/application.properties` (or via environment variables):
   ```properties
   spring.datasource.username=root
   spring.datasource.password=YOUR_MYSQL_ROOT_PASSWORD
   ```
3. Run the application:
   ```cmd
   mvn spring-boot:run
   ```

### Option B: Quick In-Memory H2 Profile (Zero Configuration)
To run instantly without database setup:
```cmd
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

4. Open your web browser and navigate to:
   **http://localhost:8080**
