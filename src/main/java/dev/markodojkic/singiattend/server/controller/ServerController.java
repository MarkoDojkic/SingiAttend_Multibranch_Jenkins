package dev.markodojkic.singiattend.server.controller;

import dev.markodojkic.singiattend.server.entity.*;
import dev.markodojkic.singiattend.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController()
@RequestMapping("api")
public class ServerController {
    @Autowired
    private ServerService serverService;

    @PostMapping("insert/staff")
    @CrossOrigin(origins = {"https://localhost:62812"})
    public Staff addNewStaffMember(@RequestBody Staff newStaff){
        return this.serverService.addNewStaffMember(newStaff);
    }

    @RequestMapping(value = "update/staff/{id}", method = RequestMethod.PATCH)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Staff updateStaffMemberById(@PathVariable String id, @RequestBody Staff newStaffData){
        return this.serverService.updateStaffMemberById(id, newStaffData);
    }

    @RequestMapping(value = "checkPassword/staff/{id}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    String checkPassword(@PathVariable String id, @RequestBody String plainPassword){
        return this.serverService.checkPassword(id, plainPassword) ? this.serverService.getStaffNameAndRole(id) : "INVALID\n";
    }

    @PostMapping("insert/student")
    @CrossOrigin(origins = {"https://localhost:62812"})
    Student addNewStudent(@RequestBody Student newStudent){
        return this.serverService.addNewStudent(newStudent);
    }

    @RequestMapping(value = "update/student/{id}", method = RequestMethod.PATCH)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Student updateStudentById(@PathVariable String id, @RequestBody Student newStudentData){
        return this.serverService.updateStudentById(id, newStudentData);
    }

    @RequestMapping(value = "checkPassword/student/{index}", method = RequestMethod.POST)
    @CrossOrigin(origins = {"https://localhost:62812"})
    String checkPasswordStudent(@PathVariable String index, @RequestBody String plainPassword){
        return this.serverService.checkPasswordStudent(index, plainPassword);
    }

    @RequestMapping(value = "getStudentName/{index}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    String getNameSurnameStudent(@PathVariable String index){
        return this.serverService.getNameSurnameStudent(index);
    }

    @RequestMapping(value = "getCourseData/{index}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<CourseDataInstance> getCourseData(@PathVariable String index) throws ParseException {
        return this.serverService.getCourseData(index);
    }

    @RequestMapping(value = "recordAttendance/{index}/{subjectId}/{isExercise}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    String recordAttendance(@PathVariable String subjectId, @PathVariable String index, @PathVariable boolean isExercise){
        return this.serverService.recordAttendance(subjectId, index, isExercise);
    }

    @PostMapping(value = "checkPassword/admin")
    @CrossOrigin(origins = {"https://localhost:62812"})
    String checkPassword(@RequestBody String plainPassword){
        return this.serverService.checkPasswordAdmin(plainPassword) ? "VALID\n" : "INVALID\n";
    }

    @RequestMapping(value = "getAttendanceData/{index}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<AttendanceDataInstance> getAttendanceData(@PathVariable String index){
        return this.serverService.getAttendanceData(index);
    }

    @RequestMapping(value = "getAllAssistants", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Staff> getAllAssistants(){
        return this.serverService.getAllAssistants();
    }

    @RequestMapping(value = "getAllStudies", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Study> getAllStudies(){
        return this.serverService.getAllStudies();
    }

    @RequestMapping(value = "addNewSubject", method = RequestMethod.POST)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Subject getAllStudies(@RequestBody Subject newSubjectData){
        return this.serverService.addNewSubject(newSubjectData);
    }

    @RequestMapping(value = "getAllStudents/{studyId}/{year}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Student> getAllStudentsBySY(@PathVariable String studyId, @PathVariable int year){
        return this.serverService.getAllStudentsBySY(studyId, year);
    }

    @RequestMapping(value = "getAllSubjectsByProfessor/{professorId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Subject> getSubjectByProfessorId(@PathVariable String professorId){
        return this.serverService.getSubjectsByProfessorId(professorId);
    }

    @RequestMapping(value = "getSubject/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Subject getSubjectById(@PathVariable String subjectId){
        return this.serverService.getSubjectById(subjectId);
    }

    @RequestMapping(value = "totalStudents/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    int totalStudentsBySubjectId(@PathVariable String subjectId){
        return this.serverService.totalStudentsBySubjectId(subjectId);
    }

    @RequestMapping(value = "getAllLectures/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Lecture> allLecturesBySubjectId(@PathVariable String subjectId){
        return this.serverService.allLecturesBySubjectId(subjectId);
    }

    @RequestMapping(value = "getAllExercises/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Exercise> allExercisesBySubjectId(@PathVariable String subjectId){
        return this.serverService.allExercisesBySubjectId(subjectId);
    }

    @RequestMapping(value = "allStudentBySubjectId/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Student> getAllStudentsBySubjectId(@PathVariable String subjectId){
        return this.serverService.allStudentBySubjectId(subjectId);
    }

    @RequestMapping(value = "update/subject/{id}", method = RequestMethod.PATCH)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Subject updateSubjectBySubjectId(@PathVariable String id, @RequestBody Subject newSubjectData){
        return this.serverService.updateSubjectBySubjectId(newSubjectData, id);
    }

    @RequestMapping(value = "subjectIsInactiveById/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    boolean subjectIsInactiveById(@PathVariable String subjectId){
        return this.serverService.subjectIsInactiveById(subjectId);
    }

    @RequestMapping(value = "getLastLecture/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Lecture getLastLecture(@PathVariable String subjectId){
        return this.serverService.getLastLecture(subjectId);
    }

    @RequestMapping(value = "insert/lecture/{subjectId}/{start}/{end}", method = RequestMethod.PUT)
    @CrossOrigin(origins = {"https://localhost:62812"})
    public void startNewLecture(@PathVariable String subjectId, @PathVariable String start, @PathVariable String end){
        this.serverService.startNewLecture(subjectId, start, end);
    }

    @RequestMapping(value = "getLecture/{lectureId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Lecture getLecture(@PathVariable String lectureId){
        return this.serverService.getLecture(lectureId);
    }

    @RequestMapping(value = "startNewSubjectYear/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    void startNewSubjectYear(@PathVariable String subjectId) {
        this.serverService.startNewSubjectYear(subjectId);
    }

    @RequestMapping(value = "endCurrentSubjectYear/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    void endCurrentSubjectYear(@PathVariable String subjectId) {
        this.serverService.endCurrentSubjectYear(subjectId);
    }

    @RequestMapping(value = "getLastExercise/{subjectId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Exercise getLastExercise(@PathVariable String subjectId){
        return this.serverService.getLastExercise(subjectId);
    }

    @RequestMapping(value = "insert/exercise/{subjectId}/{start}/{end}", method = RequestMethod.PUT)
    @CrossOrigin(origins = {"https://localhost:62812"})
    public void startNewExercise(@PathVariable String subjectId, @PathVariable String start, @PathVariable String end){
        this.serverService.startNewExercise(subjectId, start, end);
    }

    @RequestMapping(value = "getAllSubjectsByAssistant/{assistantId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Subject> getSubjectsByAssistantId(@PathVariable String assistantId){
        return this.serverService.getSubjectsByAssistantId(assistantId);
    }

    @RequestMapping(value = "getExercise/{exerciseId}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    Exercise getExercise(@PathVariable String exerciseId){
        return this.serverService.getExercise(exerciseId);
    }

    @RequestMapping(value = "getAllSubjects", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Subject> getSubjects(){
        return this.serverService.getAllSubjects();
    }

    @RequestMapping(value = "getAllStudents", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Student> getAllStudents(){
        return this.serverService.getAllStudents();
    }

    @RequestMapping(value = "getAllStaff", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    List<Staff> getAllStaff(){
        return this.serverService.getAllStaff();
    }

    @RequestMapping(value = "checkIfStaffHasSubjectAssigned/{staffId}/{isAssistant}", method = RequestMethod.GET)
    @CrossOrigin(origins = {"https://localhost:62812"})
    boolean checkIfStaffHasSubjectAssigned(@PathVariable String staffId, @PathVariable String isAssistant){
        return this.serverService.checkIfStaffHasSubjectAssigned(staffId, isAssistant.equals("1"));
    }

    @RequestMapping(value = "delete/staff/{staffId}/{isAssistant}", method = RequestMethod.DELETE)
    @CrossOrigin(origins = {"https://localhost:62812"})
    void deleteStaff(@PathVariable String staffId, @PathVariable String isAssistant){
        this.serverService.deleteStaff(staffId,isAssistant.equals("1"));
    }

    @RequestMapping(value = "delete/student/{studentId}", method = RequestMethod.DELETE)
    @CrossOrigin(origins = {"https://localhost:62812"})
    void deleteStudent(@PathVariable String studentId){
        this.serverService.deleteStudent(studentId);
    }
}
