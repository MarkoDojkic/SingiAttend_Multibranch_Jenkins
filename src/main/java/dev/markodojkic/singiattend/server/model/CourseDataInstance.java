package dev.markodojkic.singiattend.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class CourseDataInstance {
    String subjectId;
    String nameSurname;
    String subject;
    String subjectEnglish;
    String beginTime;
    String endTime;
}