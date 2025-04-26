package dev.markodojkic.singiattend.server.controller;

import dev.markodojkic.singiattend.server.model.*;
import dev.markodojkic.singiattend.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api")
@CrossOrigin(origins = {"https://localhost:8080", "https://localhost:62810"}, allowCredentials = "true")
public class ServerController {
    private final ServerService serverService;

    @Autowired
    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping("/csrfLogin")
    CsrfToken csrfLogin(CsrfToken token) {
        return token;
    }

    @PostMapping("insert/staff")
    StaffDTO addNewStaffMember(@RequestBody StaffDTO newStaff){
        return this.serverService.addNewStaffMember(newStaff);
    }

    @PatchMapping(value = "update/staff/{id}")
    StaffDTO updateStaffMemberById(@PathVariable String id, @RequestBody StaffDTO newStaffData){
        return this.serverService.updateStaffMemberById(id, newStaffData);
    }

    @PostMapping(value = "checkPassword/staff/{id}")
    String checkPasswordStaff(@PathVariable String id, @RequestBody String plainPassword){
        return this.serverService.checkPasswordStaff(id, plainPassword) ? this.serverService.getStaffNameAndRole(id) : "INVALID\n";
    }

    @PostMapping("insert/student")
    StudentDTO addNewStudent(@RequestBody StudentDTO newStudent){
        return this.serverService.addNewStudent(newStudent);
    }

    @PatchMapping(value = "update/student/{id}")
    StudentDTO updateStudentById(@PathVariable String id, @RequestBody StudentDTO newStudentData){
        return this.serverService.updateStudentById(id, newStudentData);
    }

    @PostMapping(value = "checkPassword/student/{index}")
    String checkPasswordStudent(@PathVariable String index, @RequestBody String plainPassword){
        return this.serverService.checkPasswordStudent(index, plainPassword);
    }

    @GetMapping(value = "getStudentName/{index}")
    String getNameSurnameStudent(@PathVariable String index){
        return this.serverService.getNameSurnameStudent(index);
    }

    @GetMapping(value = "getCourseData/{index}")
    List<CourseDataInstance> getCourseData(@PathVariable String index) {
        return this.serverService.getCourseData(index);
    }

    @GetMapping(value = "recordAttendance/{index}/{subjectId}/{isExercise}")
    String recordAttendance(@PathVariable String subjectId, @PathVariable String index, @PathVariable boolean isExercise){
        return this.serverService.recordAttendance(subjectId, index, isExercise);
    }

    @GetMapping(value = "getAttendanceData/{index}")
    List<AttendanceDataInstance> getAttendanceData(@PathVariable String index){
        return this.serverService.getAttendanceData(index);
    }

    @GetMapping(value = "getAllAssistants")
    List<StaffDTO> getAllAssistants(){
        return this.serverService.getAllAssistants();
    }

    @GetMapping(value = "getAllStudies")
    List<StudyDTO> getAllStudies(){
        return this.serverService.getAllStudies();
    }

    @PostMapping(value = "addNewSubject")
    SubjectDTO getAllStudies(@RequestBody SubjectDTO newSubjectData){
        return this.serverService.addNewSubject(newSubjectData);
    }

    @GetMapping(value = "getAllStudents/{studyId}/{year}")
    List<StudentDTO> getAllStudentsBySY(@PathVariable String studyId, @PathVariable int year){
        return this.serverService.getAllStudentsBySubjectIdAndAttendanceYear(studyId, year);
    }

    @GetMapping(value = "getAllSubjectsByProfessor/{professorId}")
    List<SubjectDTO> getSubjectByProfessorId(@PathVariable String professorId){
        return this.serverService.getSubjectsByProfessorId(professorId);
    }

    @GetMapping(value = "getSubject/{subjectId}")
    SubjectDTO getSubjectById(@PathVariable String subjectId) {
        return this.serverService.getSubjectById(subjectId);
    }

    @GetMapping(value = "totalStudents/{subjectId}")
    int totalStudentsBySubjectId(@PathVariable String subjectId){
        return this.serverService.totalStudentsBySubjectId(subjectId);
    }

    @GetMapping(value = "getAllLectures/{subjectId}")
    List<ClassInstanceDTO> getAllLecturesBySubjectId(@PathVariable String subjectId){
        return this.serverService.getAllClassInstancesBySubjectId(false, subjectId);
    }

    @GetMapping(value = "getAllExercises/{subjectId}")
    List<ClassInstanceDTO> getAllExercisesBySubjectId(@PathVariable String subjectId){
        return this.serverService.getAllClassInstancesBySubjectId(true, subjectId);
    }

    @GetMapping(value = "allStudentBySubjectId/{subjectId}")
    List<StudentDTO> getAllStudentsBySubjectId(@PathVariable String subjectId){
        return this.serverService.getAllStudentsBySubjectId(subjectId);
    }

    @PatchMapping(value = "update/subject/{id}")
    SubjectDTO updateSubjectBySubjectId(@PathVariable String id, @RequestBody SubjectDTO newSubjectData){
        return this.serverService.updateSubjectBySubjectId(newSubjectData, id);
    }

    @GetMapping(value = "subjectIsInactiveById/{subjectId}")
    boolean subjectIsInactiveById(@PathVariable String subjectId){
        return this.serverService.subjectIsInactiveById(subjectId);
    }

    @GetMapping(value = "getLastLecture/{subjectId}")
    ClassInstanceDTO getLastLecture(@PathVariable String subjectId){
        return this.serverService.getLastClassInstanceBySubjectId(false, subjectId);
    }

    @PutMapping(value = "insert/lecture/{subjectId}/{start}/{end}")
    public void startNewLecture(@PathVariable String subjectId, @PathVariable String start, @PathVariable String end){
        this.serverService.startNewClassInstance(false, subjectId, start, end);
    }

    @GetMapping(value = "getLecture/{lectureId}")
    ClassInstanceDTO getLecture(@PathVariable String lectureId){
        return this.serverService.getClassInstance(false, lectureId);
    }

    @GetMapping(value = "startNewSubjectYear/{subjectId}")
    void startNewSubjectYear(@PathVariable String subjectId) {
        this.serverService.startNewSubjectYear(subjectId);
    }

    @GetMapping(value = "endCurrentSubjectYear/{subjectId}")
    void endCurrentSubjectYear(@PathVariable String subjectId) {
        this.serverService.endCurrentSubjectYear(subjectId);
    }

    @GetMapping(value = "getLastExercise/{subjectId}")
    ClassInstanceDTO getLastExercise(@PathVariable String subjectId){
        return this.serverService.getLastClassInstanceBySubjectId(true, subjectId);
    }

    @PutMapping(value = "insert/exercise/{subjectId}/{start}/{end}")
    public void startNewExercise(@PathVariable String subjectId, @PathVariable String start, @PathVariable String end){
        this.serverService.startNewClassInstance(true, subjectId, start, end);
    }

    @GetMapping(value = "getAllSubjectsByAssistant/{assistantId}")
    List<SubjectDTO> getSubjectsByAssistantId(@PathVariable String assistantId){
        return this.serverService.getSubjectsByAssistantId(assistantId);
    }

    @GetMapping(value = "getExercise/{exerciseId}")
    ClassInstanceDTO getExercise(@PathVariable String exerciseId){
        return this.serverService.getClassInstance(true, exerciseId);
    }

    @GetMapping(value = "getAllSubjects")
    List<SubjectDTO> getSubjects(){
        return this.serverService.getAllSubjects();
    }

    @GetMapping(value = "getAllStudents")
    List<StudentDTO> getAllStudents(){
        return this.serverService.getAllStudents();
    }

    @GetMapping(value = "getAllStaff")
    List<StaffDTO> getAllStaff(){
        return this.serverService.getAllStaff();
    }

    @GetMapping(value = "checkIfStaffHasSubjectAssigned/{staffId}")
    boolean checkIfStaffHasSubjectAssigned(@PathVariable String staffId){
        return this.serverService.checkIfStaffHasSubjectAssigned(staffId);
    }

    @DeleteMapping(value = "delete/staff/{staffId}/{isAssistant}")
    void deleteStaff(@PathVariable String staffId, @PathVariable String isAssistant){
        this.serverService.deleteStaff(staffId,isAssistant.equals("1"));
    }

    @DeleteMapping(value = "delete/student/{studentId}")
    void deleteStudent(@PathVariable String studentId){
        this.serverService.deleteStudent(studentId);
    }
}
