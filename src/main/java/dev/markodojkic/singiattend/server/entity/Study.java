package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.*;

@Data
@Document(collection = "Studies")
public class Study {
    @Id
    private String id;
    @Field("title")
    private String title;
    @Field("title_english")
    private String title_english;
    @Field("faculty.title")
    private String faculty_title;
    @Field("faculty.title_english")
    private String faculty_title_english;
    @Field("taught_in")
    private String taught_in;
}