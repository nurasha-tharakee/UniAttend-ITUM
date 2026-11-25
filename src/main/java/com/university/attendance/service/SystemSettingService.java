package com.university.attendance.service;

import com.university.attendance.entity.SystemSetting;
import com.university.attendance.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SystemSettingService {

    public static final String KEY_CAMPUS_NAME = "CAMPUS_NAME";
    public static final String KEY_CAMPUS_LATITUDE = "GPS_CAMPUS_LATITUDE";
    public static final String KEY_CAMPUS_LONGITUDE = "GPS_CAMPUS_LONGITUDE";
    public static final String KEY_ALLOWED_RADIUS = "GPS_ALLOWED_RADIUS_METERS";
    public static final String KEY_GPS_ENFORCEMENT = "GPS_ENFORCEMENT_ENABLED";
    public static final String KEY_SERVER_BASE_URL = "SERVER_BASE_URL";
    public static final String KEY_ABSENCE_DOC_MANDATORY = "ABSENCE_DOCUMENT_MANDATORY";

    // ITUM Diyagama, Homagama, Sri Lanka coordinates
    public static final double DEFAULT_ITUM_LATITUDE = 6.80894;
    public static final double DEFAULT_ITUM_LONGITUDE = 79.99282;
    public static final double DEFAULT_ALLOWED_RADIUS_METERS = 1000.0;

    private final SystemSettingRepository settingRepository;

    public SystemSettingService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaults() {
        initIfMissing(KEY_CAMPUS_NAME, "Institute of Technology, University of Moratuwa (ITUM)", "Official Campus Name");
        initIfMissing(KEY_CAMPUS_LATITUDE, String.valueOf(DEFAULT_ITUM_LATITUDE), "ITUM Diyagama Latitude");
        initIfMissing(KEY_CAMPUS_LONGITUDE, String.valueOf(DEFAULT_ITUM_LONGITUDE), "ITUM Diyagama Longitude");
        initIfMissing(KEY_ALLOWED_RADIUS, String.valueOf((int) DEFAULT_ALLOWED_RADIUS_METERS), "Allowed attendance radius in meters");
        initIfMissing(KEY_GPS_ENFORCEMENT, "true", "Enforce GPS geofencing during attendance marking (true/false)");
        initIfMissing(KEY_SERVER_BASE_URL, "https://172.20.10.2:8443", "Configurable LAN-accessible base URL for QR codes");
        initIfMissing(KEY_ABSENCE_DOC_MANDATORY, "false", "Whether supporting document is mandatory for absence requests");

        // ── Upgrade: ensure the stored server base URL always uses HTTPS ──────
        // If a previous run stored the plain-HTTP value, migrate it now so QR
        // codes encode the correct https:// URL that mobile phones can scan.
        migrateHttpToHttps();
    }

    private void migrateHttpToHttps() {
        settingRepository.findByKey(KEY_SERVER_BASE_URL).ifPresent(setting -> {
            String val = setting.getValue();
            if (val != null && val.startsWith("http://") && !val.startsWith("https://")) {
                // Replace http:// with https:// and port 8080 → 8443
                String updated = val
                        .replace("http://", "https://")
                        .replace(":8080", ":8443");
                setting.setValue(updated);
                settingRepository.save(setting);
                System.out.println(">>> [SystemSetting] Migrated SERVER_BASE_URL to HTTPS: " + updated);
            }
        });
    }

    private void initIfMissing(String key, String defaultValue, String description) {
        if (!settingRepository.existsByKey(key)) {
            settingRepository.save(new SystemSetting(key, defaultValue, description));
        }
    }

    public List<SystemSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .filter(v -> v != null && !v.trim().isEmpty())
                .orElse(defaultValue);
    }

    @Transactional
    public void setSettingValue(String key, String value, String description) {
        Optional<SystemSetting> opt = settingRepository.findByKey(key);
        if (opt.isPresent()) {
            SystemSetting setting = opt.get();
            setting.setValue(value);
            if (description != null) {
                setting.setDescription(description);
            }
            settingRepository.save(setting);
        } else {
            settingRepository.save(new SystemSetting(key, value, description));
        }
    }

    public double getCampusLatitude() {
        try {
            return Double.parseDouble(getSettingValue(KEY_CAMPUS_LATITUDE, String.valueOf(DEFAULT_ITUM_LATITUDE)));
        } catch (NumberFormatException e) {
            return DEFAULT_ITUM_LATITUDE;
        }
    }

    public double getCampusLongitude() {
        try {
            return Double.parseDouble(getSettingValue(KEY_CAMPUS_LONGITUDE, String.valueOf(DEFAULT_ITUM_LONGITUDE)));
        } catch (NumberFormatException e) {
            return DEFAULT_ITUM_LONGITUDE;
        }
    }

    public double getAllowedRadiusMeters() {
        try {
            return Double.parseDouble(getSettingValue(KEY_ALLOWED_RADIUS, String.valueOf(DEFAULT_ALLOWED_RADIUS_METERS)));
        } catch (NumberFormatException e) {
            return DEFAULT_ALLOWED_RADIUS_METERS;
        }
    }

    public boolean isGpsEnforcementEnabled() {
        return "true".equalsIgnoreCase(getSettingValue(KEY_GPS_ENFORCEMENT, "true"));
    }

    public String getServerBaseUrl() {
        return getSettingValue(KEY_SERVER_BASE_URL, "https://172.20.10.2:8443");
    }

    public boolean isAbsenceDocMandatory() {
        return "true".equalsIgnoreCase(getSettingValue(KEY_ABSENCE_DOC_MANDATORY, "false"));
    }

    /**
     * Calculate geodesic distance in meters using Haversine formula
     */
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
