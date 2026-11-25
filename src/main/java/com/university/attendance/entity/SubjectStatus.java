package com.university.attendance.entity;

public enum SubjectStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ARCHIVED("Archived");

    private final String displayName;

    SubjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
