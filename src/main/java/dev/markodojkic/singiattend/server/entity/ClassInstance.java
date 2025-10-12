package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ClassInstance {
    @Id
    private String id;
    @Field("subject_id")
    private String subjectId;
    @Field("started_at")
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date startedAt;
    @Field("ended_at")
    @DateTimeFormat(style = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date endedAt;
    @Field("attended_students")
    private List<String> attendedStudents = Collections.emptyList();

    public ClassInstance(String subjectId, Date startedAt, Date endedAt) {
        this.subjectId = subjectId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }
}