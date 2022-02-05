package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.*;

@Data
@Document(collection = "Staff")
public class Staff {
    @Id
    private String id;
    @Field("name_surname")
    private String name_surname;
    @Field("email")
    private String email;
    @Field("password_hash")
    private String password_hash;
    @Field("role")
    private String role; //Enum: 'assistant','professor'

    public Staff(String id, String name_surname, String email, String password_hash, String role) {
        this.id = id;
        this.name_surname = name_surname;
        this.email = email;
        this.password_hash = password_hash;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName_surname() {
        return name_surname;
    }

    public void setName_surname(String name_surname) {
        this.name_surname = name_surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword_hash() {
        return password_hash;
    }

    public void setPassword_hash(String password_hash) {
        this.password_hash = password_hash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}