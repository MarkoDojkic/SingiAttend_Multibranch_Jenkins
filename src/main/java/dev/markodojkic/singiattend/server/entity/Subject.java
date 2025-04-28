package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "Subjects")
public class Subject {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String titleEnglish;
    @Field(targetType = FieldType.OBJECT_ID, value = "professor_id")
    private String professorId;
    @Field(targetType = FieldType.OBJECT_ID, value = "assistant_id")
    private String assistantId;
    @Field("last_lecture_at")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date lastLectureAt;
    @Field("last_exercise_at")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date lastExerciseAt;
    @Field("enrolled_student_ids")
    private List<String> enrolledStudentIds = Collections.emptyList();
    @Field("isInactive")
    private Boolean isInactive;
}