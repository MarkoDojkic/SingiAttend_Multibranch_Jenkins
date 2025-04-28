package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;

@Data
public class AttendanceHelperInstance {
    @Id
    @Field("_id")
    private String subjectId;
    @Field("title")
    private String title;
    @Field("title_english")
    private String titleEnglish;
    @Field("isInactive")
    private Boolean isInactive;
    private String nameT;
    private String nameA;
}
