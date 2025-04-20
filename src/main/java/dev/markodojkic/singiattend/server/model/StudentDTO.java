package dev.markodojkic.singiattend.server.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Student}
 */
@Data
@Getter
@Setter
public class StudentDTO implements Serializable {
    String id;
    String nameSurname;
    String index;
    String passwordHash;
    String email;
    String studyId;
    String year;
    transient List<StudyDTO> study;
}