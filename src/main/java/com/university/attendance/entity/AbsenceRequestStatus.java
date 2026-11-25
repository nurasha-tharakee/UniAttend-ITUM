package com.university.attendance.entity;

public enum AbsenceRequestStatus {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    NEEDS_CLARIFICATION("Needs Clarification");

    private final String displayName;

    AbsenceRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
