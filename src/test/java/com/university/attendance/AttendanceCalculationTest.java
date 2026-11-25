package com.university.attendance;

import com.university.attendance.dto.SubjectAttendanceStatsDto;
import com.university.attendance.entity.ClassSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceCalculationTest {

    @Test
    @DisplayName("Formula test: (Present / Total) * 100")
    void testAttendancePercentageCalculation() {
        // 8 present out of 10 conducted -> 80.0%
        SubjectAttendanceStatsDto stats = new SubjectAttendanceStatsDto(1L, "CS101", "Algorithms", "Dr. Smith", 10, 8);
        assertEquals(80.0, stats.getPercentage());
        assertEquals(2, stats.getAbsentCount());
        assertEquals("success", stats.getStatusBadge());

        // 7 present out of 10 conducted -> 70.0%
        SubjectAttendanceStatsDto lowStats = new SubjectAttendanceStatsDto(2L, "SE201", "Software Eng", "Prof. Johnson", 10, 7);
        assertEquals(70.0, lowStats.getPercentage());
        assertEquals(3, lowStats.getAbsentCount());
        assertEquals("danger", lowStats.getStatusBadge());

        // 0 conducted -> 100.0% (no classes conducted yet)
        SubjectAttendanceStatsDto zeroStats = new SubjectAttendanceStatsDto(3L, "CS301", "Databases", "Dr. Smith", 0, 0);
        assertEquals(100.0, zeroStats.getPercentage());
        assertEquals(0, zeroStats.getAbsentCount());
    }

    @Test
    @DisplayName("QR Expiration test: Valid within 2 minutes, expired after")
    void testQrTokenExpiration() {
        ClassSession session = new ClassSession();
        session.setQrToken("TEST-TOKEN-123");
        session.setQrTokenExpiresAt(LocalDateTime.now().plusMinutes(2));

        // Right now should NOT be expired
        assertFalse(session.isQrExpired());

        // Set to past time
        session.setQrTokenExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertTrue(session.isQrExpired());
    }

    @Test
    @DisplayName("Haversine GPS test: Diyagama campus perimeter calculation")
    void testItumCampusGeofencingDistance() {
        com.university.attendance.service.SystemSettingService service =
                new com.university.attendance.service.SystemSettingService(null);

        double itumLat = 6.80894;
        double itumLon = 79.99282;

        // Same point distance should be 0
        double distZero = service.calculateDistanceMeters(itumLat, itumLon, itumLat, itumLon);
        assertEquals(0.0, distZero, 0.01);

        // Within 100m on campus grounds
        double nearbyLat = 6.80920;
        double nearbyLon = 79.99300;
        double distNearby = service.calculateDistanceMeters(itumLat, itumLon, nearbyLat, nearbyLon);
        assertTrue(distNearby < 100.0, "Nearby coordinate should be within 100m");

        // Remote location (Colombo Fort ~21km away)
        double colomboFortLat = 6.9344;
        double colomboFortLon = 79.8428;
        double distRemote = service.calculateDistanceMeters(itumLat, itumLon, colomboFortLat, colomboFortLon);
        assertTrue(distRemote > 15000.0, "Colombo Fort should be > 15km away from ITUM Diyagama");
    }
}
