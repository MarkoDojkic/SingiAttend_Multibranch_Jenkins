package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.hibernate.annotations.Type;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.persistence.*;
import java.util.ArrayList;

@Data
@Document(collection = "Subjects")
public class Subject {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String title_english;
    @Field(targetType = FieldType.OBJECT_ID, value = "professor_id")
    private String professorId;
    @Field(targetType = FieldType.OBJECT_ID, value = "assistant_id")
    private String assistantId;
    @Field("last_lecture_at")
    private String lastLectureAt;
    @Field("last_exercise_at")
    private String lastExerciseAt;
    @Type(type = "string-array")
    @Field("enroled_students")
    private ArrayList<String> enroled_students;
    @Field("isInactive")
    private String isInactive;
    @Field("professor")
    private ArrayList<Staff> professor;
    @Field("assistant")
    private ArrayList<Staff> assistant;
}