package dev.markodojkic.singiattend.student_proxy.model;

import lombok.Data;
import java.io.Serializable;

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