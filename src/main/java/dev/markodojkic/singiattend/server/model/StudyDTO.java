package dev.markodojkic.singiattend.server.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Study}
 */
@Data
@Getter
@Setter
public class StudyDTO implements Serializable {
    String id;
    String title;
    String titleEnglish;
    String facultyTitle;
    String facultyTitleEnglish;
    String taughtIn;
}