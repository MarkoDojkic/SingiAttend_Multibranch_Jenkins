package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;

@Data
@Document(collection = "Studies")
public class Study {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String titleEnglish;
    @Field("faculty.title")
    private String facultyTitle;
    @Field("faculty.title_english")
    private String facultyTitleEnglish;
    @Field("taught_in")
    private String taughtIn;
}