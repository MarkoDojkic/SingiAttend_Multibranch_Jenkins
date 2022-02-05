package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.hibernate.annotations.Type;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.util.List;

@Document(collection="Subjects")
@Data
public class CourseDataLecture {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String title_english;
    @Field("last_lecture_at")
    private String last_lecture_at;
    @Field("last_exercise_at")
    private String last_exercise_at;
    @Type(type = "list-array")
    @Field("professor")
    private List<Staff> professor;
    @Type(type = "list-array")
    @Field("assistant")
    private List<Staff> assistant;

    /*public CourseDataLecture(String id, String title, String title_english, String last_lecture_at, String last_exercise_at, ArrayList<Staff> professor, ArrayList<Staff> assistant) {
        this.id = id;
        this.title = title;
        this.title_english = title_english;
        this.last_lecture_at = last_lecture_at;
        this.last_exercise_at = last_exercise_at;
        this.professor = professor;
        this.assistant = assistant;
    }*/
}
