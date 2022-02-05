package dev.markodojkic.singiattend.server.service;

import dev.markodojkic.singiattend.server.entity.*;
import java.text.ParseException;
import java.util.List;

public interface IServerService {
    Staff addNewStaffMember(Staff newStaff, boolean isUpdate);
    Staff updateStaffMemberById(String id, Staff newStaffData);
    boolean checkPassword(String id, String plainPassword);
    Student addNewStudent(Student newStudent, boolean isUpdate);
    Student updateStudentById(String id, Student newStudentData);
    String checkPasswordStudent(String index, String plainPassword);
    String getNameSurnameStudent(String index);
    List<CourseDataInstance> getCourseData(String index) throws ParseException;
    String recordAttendance(String subjectId, String index, boolean isExercise);
    boolean checkPasswordAdmin(String plainPassword);
    List<AttendanceDataInstance> getAttendanceData(String index);
    //PHP SQL QUERY USE THESE *UPDATE IN PHP!!*
    List<Subject> getSubjectsByProfessorId(String professorId); //Aggregation
    List<Subject> getSubjectsByAssistantId(String assistantId); //Aggregation
    List<Staff> getAllStaff(); //Query
    List<Study> getAllStudies(); //Query
    List<Student> getAllStudents(); //Aggregation
    List<Subject> getAllSubjects(); //Query
    Subject addNewSubject(Subject newSubject); //Insert
    int totalStudentsBySubjectId(String subjectId); //Return total number of students
    List<Student> getAllStudentsBySY(String studyId, int year); //Aggregation
    Subject getSubjectById(String subjectId); //Aggregation
    Staff getStaffMemberById(String id);
    boolean checkIfStaffHasSubjectAssigned(String id, boolean isAssistant); //Check against as professor or assistant depending on flag
    void deleteStaff(String staffId, boolean isAssistant); //Delete staff, and if professor delete all subjects
    void deleteStudent(String studentId); //Delete student
    List<Student> allStudentBySubjectId(String subjectId); //Query
    boolean subjectIsInactiveById(String subjectId); //Query
    List<Exercise> allExercisesBySubjectId(String subjectId); //Query
    Exercise getLastExercise(String subjectId); //Query
    void startNewLecture(String subjectId, String begin, String end);
    void startNewExercise(String subjectId, String begin, String end);
    List<Staff> getAllAsistants();
    void startNewSubjectYear(String subjectId);
    void endCurrentSubjectYear(String subjectId);
    Subject updateSubjectBySubjectId(Subject newSubjectData, String subjectId);
    List<Lecture> allLecturesBySubjectId(String subjectId); //Query
    Lecture getLastLecture(String subjectId); //Query
    String getStaffNameAndRole(String staffId);
    Lecture getLecture(String lectureId);
    Exercise getExercise(String exerciseId);
}
