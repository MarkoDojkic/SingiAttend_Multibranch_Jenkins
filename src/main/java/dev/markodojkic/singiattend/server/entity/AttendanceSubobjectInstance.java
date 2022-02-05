package dev.markodojkic.singiattend.server.entity;

import lombok.Data;
import org.hibernate.annotations.Type;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.util.ArrayList;

@Data
public class AttendanceSubobjectInstance {
    @Id
    @Field("_id")
    private String subjectId;
    @Field("title")
    private String title;
    @Field("title_english")
    private String titleEnglish;
    @Field("isInactive")
    private String isInactive;
    private String nameT;
    private String nameA;
    @Type(type = "array")
    @Field("professor")
    private ArrayList<Staff> professor;
    @Type(type = "array")
    @Field("assistant")
    private ArrayList<Staff> assistant;

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public void setTitleEnglish(String titleEnglish) {
        this.titleEnglish = titleEnglish;
    }

    public String isInactive() { return isInactive; }

    public void setInactive(String inactive) {
        isInactive = inactive;
    }

    public String getNameT() {
        return nameT;
    }

    public void setNameT(String nameT) {
        this.nameT = nameT;
    }

    public String getNameA() {
        return nameA;
    }

    public void setNameA(String nameA) {
        this.nameA = nameA;
    }

    public ArrayList<Staff> getProfessor() {
        return professor;
    }

    public void setProfessor(ArrayList<Staff> professor) {
        this.professor = professor;
    }

    public ArrayList<Staff> getAssistant() {
        return assistant;
    }

    public void setAssistant(ArrayList<Staff> assistant) {
        this.assistant = assistant;
    }
}
