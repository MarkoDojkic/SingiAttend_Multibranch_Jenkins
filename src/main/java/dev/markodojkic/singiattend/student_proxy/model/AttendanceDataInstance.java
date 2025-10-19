package dev.markodojkic.singiattend.student_proxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceDataInstance {
    AttendanceHelperInstance attendanceHelperInstance;
    int attendedLectures;
    int attendedPractices;
    int totalLectures;
    int totalPractices;
}
