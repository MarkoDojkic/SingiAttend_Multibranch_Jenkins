package dev.markodojkic.singiattend.server.entity;
public class CourseDataInstance {
    String subjectId;
    String nameSurname;
    String subject;
    String subjectEnglish;
    String beginTime;
    String endTime;

    public CourseDataInstance(String subjectId, String nameSurname, String subject, String subjectEnglish, String beginTime, String endTime) {
        this.subjectId = subjectId;
        this.nameSurname = nameSurname;
        this.subject = subject;
        this.subjectEnglish = subjectEnglish;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getNameSurname() {
        return nameSurname;
    }

    public void setNameSurname(String nameSurname) {
        this.nameSurname = nameSurname;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubjectEnglish() {
        return subjectEnglish;
    }

    public void setSubjectEnglish(String subjectEnglish) {
        this.subjectEnglish = subjectEnglish;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}