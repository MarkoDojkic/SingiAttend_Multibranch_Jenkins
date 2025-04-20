package dev.markodojkic.singiattend.server.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Subject}
 */
@Data
@Getter
@Setter
public class SubjectDTO implements Serializable {
    String id;
    String title;
    String titleEnglish;
    String professorId;
    String assistantId;
    Date lastLectureAt;
    Date lastExerciseAt;
    transient List<String> enrolledStudentIds;
    String isInactive;
    transient List<StaffDTO> professor;
    transient List<StaffDTO> assistant;
}