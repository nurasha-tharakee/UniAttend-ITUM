package com.university.attendance.entity;

public enum AbsenceReasonCategory {
    MEDICAL_ILLNESS("Medical / Illness"),
    FAMILY_EMERGENCY("Family Emergency"),
    PERSONAL_REASON("Personal Reason"),
    TRANSPORT_PROBLEM("Transport Problem"),
    SPORTS_EVENT("University Sports Event"),
    OFFICIAL_ACTIVITY("Official University Activity"),
    TECHNICAL_PROBLEM("Technical Problem"),
    OTHER("Other Reason");

    private final String displayName;

    AbsenceReasonCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
