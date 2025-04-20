package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
public class ClassInstance {
    @Id
    private String id;
    @Field("subject_id")
    private String subjectId;
    @Field("started_at")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date startedAt;
    @Field("ended_at")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date endedAt;
    @Field("attended_students")
    private List<String> attendedStudents;

    public ClassInstance(String subjectId, Date startedAt, Date endedAt) {
        this.subjectId = subjectId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.attendedStudents = Collections.emptyList();
    }
}