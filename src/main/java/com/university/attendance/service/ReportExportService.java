package com.university.attendance.service;

import com.university.attendance.entity.Attendance;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportAttendanceCsv(List<Attendance> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("Student ID,Student Name,Discipline,Batch,Subject Code,Subject Name,Semester,Lecturer,Session,Session Date,Marked At,IP Address\n");
        for (Attendance a : records) {
            sb.append(csvEscape(a.getStudent().getStudentIdNumber())).append(',')
              .append(csvEscape(a.getStudent().getFullName())).append(',')
              .append(csvEscape(a.getStudent().getDepartment())).append(',')
              .append(csvEscape(a.getStudent().getBatchIntake())).append(',')
              .append(csvEscape(a.getSession().getSubject().getCode())).append(',')
              .append(csvEscape(a.getSession().getSubject().getName())).append(',')
              .append(csvEscape(a.getSession().getSubject().getSemester())).append(',')
              .append(csvEscape(a.getSession().getLecturer() != null ? a.getSession().getLecturer().getFullName() : "")).append(',')
              .append(csvEscape(a.getSession().getSessionName())).append(',')
              .append(csvEscape(a.getSession().getSessionDate() != null ? a.getSession().getSessionDate().toString() : "")).append(',')
              .append(csvEscape(a.getMarkedAt() != null ? a.getMarkedAt().format(FMT) : "")).append(',')
              .append(csvEscape(a.getIpAddress())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) return "\"\"";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Generates a clean, valid PDF 1.4 attendance report document without external dependencies.
     */
    public byte[] exportAttendancePdf(List<Attendance> records, String filterSummary) {
        List<String> lines = new ArrayList<>();
        lines.add("UniAttend - Institute of Technology, University of Moratuwa (ITUM)");
        lines.add("Attendance Report (" + (filterSummary != null ? filterSummary : "All Records") + ")");
        lines.add("Total Records: " + records.size());
        lines.add("------------------------------------------------------------------------------------");
        lines.add(String.format("%-14s %-22s %-10s %-18s %-16s", "Student ID", "Name", "Subject", "Discipline", "Marked At"));
        lines.add("------------------------------------------------------------------------------------");

        int maxRows = Math.min(records.size(), 45);
        for (int i = 0; i < maxRows; i++) {
            Attendance a = records.get(i);
            String id = truncate(a.getStudent().getStudentIdNumber(), 13);
            String name = truncate(a.getStudent().getFullName(), 21);
            String sub = truncate(a.getSession().getSubject().getCode(), 9);
            String disc = truncate(a.getStudent().getDepartment(), 17);
            String time = a.getMarkedAt() != null ? a.getMarkedAt().format(FMT) : "";
            lines.add(String.format("%-14s %-22s %-10s %-18s %-16s", id, name, sub, disc, time));
        }
        if (records.size() > maxRows) {
            lines.add("... (" + (records.size() - maxRows) + " more records; download CSV for full dataset)");
        }

        return buildSimplePdf(lines);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }

    private byte[] buildSimplePdf(List<String> lines) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 9 Tf\n40 760 Td\n13 TL\n");
        for (int i = 0; i < lines.size(); i++) {
            String safe = lines.get(i)
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replaceAll("[^\\x20-\\x7E]", "?");
            if (i == 0) {
                stream.append("(").append(safe).append(") Tj\n");
            } else {
                stream.append("T* (").append(safe).append(") Tj\n");
            }
        }
        stream.append("ET\n");
        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        writeAscii(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
        out.writeBytes(streamBytes);
        writeAscii(out, "endstream\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n");

        int xrefPos = out.size();
        writeAscii(out, "xref\n0 6\n0000000000 65535 f \n");
        for (int off : offsets) {
            writeAscii(out, String.format("%010d 00000 n \n", off));
        }
        writeAscii(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefPos + "\n%%EOF\n");
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.US_ASCII));
    }
}
