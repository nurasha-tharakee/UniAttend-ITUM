package com.university.attendance.entity;

public enum RegistrationStatus {
    PENDING_VERIFICATION("Pending Verification"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    RegistrationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
