package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Data
public class CourseDataSubjectInstance {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String titleEnglish;
    @Field("last_lecture_at")
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime lastLectureAt;
    @Field("last_exercise_at")
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime lastExerciseAt;
    private String nameT;
    private String nameA;
}
