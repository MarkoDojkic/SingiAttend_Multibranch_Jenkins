package dev.markodojkic.singiattend.server.service;

import dev.markodojkic.singiattend.server.model.*;

import java.text.ParseException;
import java.util.List;

public interface IServerService {
    StaffDTO addNewStaffMember(StaffDTO newStaff);
    StaffDTO updateStaffMemberById(String staffId, StaffDTO newStaff);
    boolean checkPassword(String staffId, String plainPassword);
    StudentDTO addNewStudent(StudentDTO newStudent);
    StudentDTO updateStudentById(String studentId, StudentDTO newStudent);
    String checkPasswordStudent(String index, String plainPassword);
    String getNameSurnameStudent(String index);
    List<CourseDataInstance> getCourseData(String index) throws ParseException;
    String recordAttendance(String subjectId, String index, boolean isExercise);
    boolean checkPasswordAdmin(String plainPassword);
    List<AttendanceDataInstance> getAttendanceData(String index);
    List<SubjectDTO> getSubjectsByProfessorId(String professorId); //Aggregation
    List<SubjectDTO> getSubjectsByAssistantId(String assistantId); //Aggregation
    List<StaffDTO> getAllStaff(); //Query
    List<StudyDTO> getAllStudies(); //Query
    List<StudentDTO> getAllStudents(); //Aggregation
    List<SubjectDTO> getAllSubjects(); //Query
    SubjectDTO addNewSubject(SubjectDTO newSubject); //Insert
    int totalStudentsBySubjectId(String subjectId); //Return total number of StudentDTOs
    List<StudentDTO> getAllStudentsBySubjectIdAndAttendanceYear(String studyId, int attendanceYear); //Aggregation
    SubjectDTO getSubjectById(String subjectId); //Aggregation
    boolean checkIfStaffHasSubjectAssigned(String staffId);
    void deleteStaff(String staffId, boolean isAssistant); //Delete StaffDTO, and if professor delete all SubjectDTOs
    void deleteStudent(String studentId); //Delete StudentDTO
    List<StudentDTO> getAllStudentsBySubjectId(String subjectId); //Query
    boolean subjectIsInactiveById(String subjectId); //Query
    List<ClassInstanceDTO> getAllClassInstancesBySubjectId(boolean isExercise, String subjectId); //Query
    ClassInstanceDTO getLastClassInstanceBySubjectId(boolean isExercise, String subjectId); //Query
    void startNewClassInstance(boolean isExercise, String subjectId, String begin, String end);
    List<StaffDTO> getAllAssistants();
    void startNewSubjectYear(String subjectId);
    void endCurrentSubjectYear(String subjectId);
    SubjectDTO updateSubjectBySubjectId(SubjectDTO newSubject, String subjectId);
    String getStaffNameAndRole(String staffId);
    ClassInstanceDTO getClassInstance(boolean isExercise, String id);
}
