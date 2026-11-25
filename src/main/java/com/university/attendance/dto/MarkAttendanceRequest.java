package com.university.attendance.dto;

public class MarkAttendanceRequest {
    private String qrToken;
    private String deviceInfo;
    private Double latitude;
    private Double longitude;

    public MarkAttendanceRequest() {
    }

    public MarkAttendanceRequest(String qrToken, String deviceInfo) {
        this.qrToken = qrToken;
        this.deviceInfo = deviceInfo;
    }

    public MarkAttendanceRequest(String qrToken, String deviceInfo, Double latitude, Double longitude) {
        this.qrToken = qrToken;
        this.deviceInfo = deviceInfo;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
