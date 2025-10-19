package dev.markodojkic.singiattend.student_proxy.model;

import lombok.Data;

@Data
public class AttendanceHelperInstance {
    private String subjectId;
    private String title;
    private String titleEnglish;
    private Boolean isInactive;
    private String nameT;
    private String nameA;
}
