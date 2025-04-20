package dev.markodojkic.singiattend.server.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * DTO for {@link dev.markodojkic.singiattend.server.entity.Staff}
 */
@Data
@Getter
@Setter
public class StaffDTO implements Serializable {
    String id;
    String nameSurname;
    String email;
    String passwordHash;
    String role; //Enum: 'assistant','professor'
}