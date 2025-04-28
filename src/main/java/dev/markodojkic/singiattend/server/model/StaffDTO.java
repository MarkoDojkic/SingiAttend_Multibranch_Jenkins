package dev.markodojkic.singiattend.server.model;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Staff}
 */
@Data
public class StaffDTO implements Serializable {
    private String id;
    private String nameSurname;
    private String email;
    private String passwordHash;
    private String role; //Used either 'assistant' or 'professor'
}