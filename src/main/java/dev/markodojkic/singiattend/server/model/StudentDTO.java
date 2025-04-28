package dev.markodojkic.singiattend.server.model;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Student}
 */
@Data
public class StudentDTO implements Serializable {
    private String id;
    private String nameSurname;
    private String index;
    private String passwordHash;
    private String email;
    private String studyId;
    private String year;
    private StudyDTO study;
}