package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Document(collection="Subjects")
@Data
public class CourseDataSubjectInstance {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String titleEnglish;
    @Field("last_lecture_at")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date lastLectureAt;
    @Field("last_exercise_at")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date lastExerciseAt;
    @Field("professor")
    private Staff professor;
    @Field("assistant")
    private Staff assistant;
}
