@echo off
title UniAttend - University QR Attendance System
echo ==============================================================
echo       UniAttend - University QR Attendance Management System
echo ==============================================================
echo Starting application on http://localhost:8080 ...
echo.

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot
set PATH=%JAVA_HOME%\bin;C:\apache-maven-3.9.6\bin;%PATH%

start http://localhost:8080/login

"%JAVA_HOME%\bin\java.exe" -jar "%~dp0target\qr-attendance-1.0.0.jar"
pause
