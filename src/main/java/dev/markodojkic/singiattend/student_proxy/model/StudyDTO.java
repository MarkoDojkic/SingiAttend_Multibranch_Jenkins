package dev.markodojkic.singiattend.student_proxy.model;

import lombok.Data;
import java.io.Serializable;

@Data
public class StudyDTO implements Serializable {
    private String id;
    private String title;
    private String titleEnglish;
    private String facultyTitle;
    private String facultyTitleEnglish;
    private String taughtIn;
}