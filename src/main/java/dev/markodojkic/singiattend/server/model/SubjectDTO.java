package dev.markodojkic.singiattend.server.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Subject}
 */
@Data
public class SubjectDTO implements Serializable {
    private String id;
    private String title;
    private String titleEnglish;
    private String professorId;
    private String assistantId;
    private Date lastLectureAt;
    private Date lastExerciseAt;
    private transient List<String> enrolledStudentIds;
    private Boolean isInactive;
}