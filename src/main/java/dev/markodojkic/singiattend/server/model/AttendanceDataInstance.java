package dev.markodojkic.singiattend.server.model;

import dev.markodojkic.singiattend.server.entity.AttendanceHelperInstance;
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
