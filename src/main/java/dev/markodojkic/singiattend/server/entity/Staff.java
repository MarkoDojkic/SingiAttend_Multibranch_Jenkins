package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;

@Data
@Document(collection = "Staff")
public class Staff {
    @Id
    private String id;
    @Field("name_surname")
    private String nameSurname;
    @Field("email")
    private String email;
    @Field("password_hash")
    private String passwordHash;
    @Field("role")
    private String role;
}