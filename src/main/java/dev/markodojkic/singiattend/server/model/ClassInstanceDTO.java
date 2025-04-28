package dev.markodojkic.singiattend.server.model;

import dev.markodojkic.singiattend.server.entity.ClassInstance;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * DTO for {@link ClassInstance}
 */
@Data
public class ClassInstanceDTO implements Serializable {
    private String id;
    private String subjectId;
    private Date startedAt;
    private Date endedAt;
    private transient List<String> attendedStudents;
}