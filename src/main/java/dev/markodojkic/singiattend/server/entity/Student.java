package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.persistence.Id;
import java.util.List;

@Data
@Document(collection = "Students")
public class Student {
    @Id
    private String id;
    @Field("name_surname")
    private String nameSurname;
    @Field("index")
    private String index;
    @Field("password_hash")
    private String passwordHash;
    @Field("email")
    private String email;
    @Field(targetType = FieldType.OBJECT_ID, value = "study_id")
    private String studyId;
    @Field("year")
    private String year;
    @Field("study")
    private List<Study> study;
}