package dev.markodojkic.singiattend.server.model;

import dev.markodojkic.singiattend.server.entity.ClassInstance;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * DTO for {@link ClassInstance}
 */
@Data
@Getter
@Setter
public class ClassInstanceDTO implements Serializable {
    String id;
    String subjectId;
    Date startedAt;
    Date endedAt;
    transient List<String> attendedStudents;
}