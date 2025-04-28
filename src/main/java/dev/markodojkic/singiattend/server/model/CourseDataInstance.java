package dev.markodojkic.singiattend.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseDataInstance {
    private String subjectId;
    private String nameSurname;
    private String subject;
    private String subjectEnglish;
    private String beginTime;
    private String endTime;
}