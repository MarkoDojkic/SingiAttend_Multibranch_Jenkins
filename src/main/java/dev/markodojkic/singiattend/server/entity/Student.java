package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.*;
import java.util.ArrayList;

@Data
@Document(collection = "Students")
public class Student {
    @Id
    private String id;
    @Field("name_surname")
    private String name_surname;
    @Field("index")
    private String index;
    @Field("password_hash")
    private String password_hash;
    @Field("email")
    private String email;
    @Field("study_id")
    private String studyId;
    @Field("year")
    private String year;
    @Field("study")
    private ArrayList<Study> study;
}