package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Collections;
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
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime lastLectureAt;
    @Field("last_exercise_at")
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime lastExerciseAt;
    @Field("enrolled_study_ids")
    private List<String> enrolledStudyIds = Collections.emptyList(); //format studyID_takingYear
    @Field("isInactive")
    private Boolean isInactive;
}