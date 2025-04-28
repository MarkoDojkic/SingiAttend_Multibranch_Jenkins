package dev.markodojkic.singiattend.server.model;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Study}
 */
@Data
public class StudyDTO implements Serializable {
    private String id;
    private String title;
    private String titleEnglish;
    private String facultyTitle;
    private String facultyTitleEnglish;
    private String taughtIn;
}