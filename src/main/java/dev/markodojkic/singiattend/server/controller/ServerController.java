package dev.markodojkic.singiattend.server.controller;

import dev.markodojkic.singiattend.server.auth.AuthorityResolver;
import dev.markodojkic.singiattend.server.entity.AttendanceHelperInstance;
import dev.markodojkic.singiattend.server.model.*;
import dev.markodojkic.singiattend.server.saml.SamlUtils;
import dev.markodojkic.singiattend.server.service.ServerService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1")
public class ServerController {
    @Value("${allowed-origins}")
    private String allowedOrigins;

    private final ServerService serverService;
    private final SessionRegistry sessionRegistry;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public ServerController(ServerService serverService, SessionRegistry sessionRegistry, SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.serverService = serverService;
        this.sessionRegistry = sessionRegistry;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping(value = "csrfLogin", consumes = "application/xml")
    CsrfToken csrfLogin(CsrfToken token, @RequestParam("loginFor") String loginFor, HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) String samlLoginResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        if (loginFor == null || loginFor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "loginFor is required");
        }

        try {
            if (loginFor.equalsFoldCase("tempAdmin")) {
                auth = new UsernamePasswordAuthenticationToken(loginFor, auth.getCredentials(), Collections.singletonList(new SimpleGrantedAuthority("TEMP_ADMIN")));
            } else if (StringUtils.isNotEmpty(samlLoginResponse)) {
                auth = new UsernamePasswordAuthenticationToken(loginFor, auth.getCredentials(), AuthorityResolver.resolve(loginFor, request, allowedOrigins, SamlUtils.validate(samlLoginResponse)));
            } else {
                auth = new UsernamePasswordAuthenticationToken(loginFor, auth.getCredentials(), AuthorityResolver.resolve(loginFor, request, allowedOrigins, false));
            }

            SecurityContextHolder.getContext().setAuthentication(auth);

            sessionAuthenticationStrategy.onAuthentication(auth, request, response);

            return token;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();

            request.getSession(false).invalidate();

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Session upgrade failed", e);
        }
    }

    @PostMapping("insert/staff")
    @PreAuthorize("hasAuthority('ADMIN')")
    StaffDTO addNewStaffMember(@RequestBody StaffDTO newStaff){
        return this.serverService.addNewStaffMember(newStaff);
    }

    @PatchMapping(value = "update/staff/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    StaffDTO updateStaffMemberById(@PathVariable String id, @RequestBody StaffDTO newStaffData){
        return this.serverService.updateStaffMemberById(id, newStaffData);
    }

    @PostMapping(value = "checkPassword/staff/{id}")
    @PreAuthorize("hasAuthority('UNAUTHENTICATED_STAFF')")
    String checkPasswordStaff(@PathVariable String id, @RequestBody String plainPassword){
        boolean result = this.serverService.checkPasswordStaff(id, plainPassword);
        if(result){
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, Collections.singletonList(new SimpleGrantedAuthority("STAFF"))));
        }
        return result ? this.serverService.getStaffNameAndRole(id) : "INVALID\n";
    }

    @PostMapping("insert/student")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TEMP_ADMIN')")
    StudentDTO addNewStudent(@RequestBody StudentDTO newStudent){
        return this.serverService.addNewStudent(newStudent);
    }

    @PatchMapping(value = "update/student/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    StudentDTO updateStudentById(@PathVariable String id, @RequestBody StudentDTO newStudentData){
        return this.serverService.updateStudentById(id, newStudentData);
    }

    @PostMapping(value = "checkPassword/student/{index}")
    @PreAuthorize("hasAuthority('UNAUTHENTICATED_STUDENT')")
    String checkPasswordStudent(@PathVariable String index, @RequestBody String plainPassword){
        String response = this.serverService.checkPasswordStudent(index, plainPassword);
        if(response.equals("VALID")) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(index, null, List.of(new SimpleGrantedAuthority("STUDENT"))));
        }
        return response;
    }

    @GetMapping(value = "getStudentName/{index}")
    @PreAuthorize("hasAnyAuthority('ADMIN','STUDENT')")
    String getNameSurnameStudent(@PathVariable String index){
        return this.serverService.getNameSurnameStudent(index);
    }

    @GetMapping(value = "getCourseData/{index}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF', 'STUDENT')")
    List<CourseDataInstance> getCourseData(@PathVariable String index) {
        return this.serverService.getCourseData(index);
    }

    @GetMapping(value = "recordAttendance/{index}/{id}/{isExercise}")
    @PreAuthorize("hasAuthority('STUDENT')")
    String recordAttendance(@PathVariable String id, @PathVariable String index, @PathVariable boolean isExercise){
        return this.serverService.recordAttendance(id, index, isExercise);
    }

    @GetMapping(value = "getAttendanceData/{index}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF', 'STUDENT')")
    List<AttendanceDataInstance> getAttendanceData(@PathVariable String index){
        return this.serverService.getAttendanceData(index);
    }

    @GetMapping(value = "getAllAssistants")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<StaffDTO> getAllAssistants(){
        return this.serverService.getAllAssistants();
    }

    @GetMapping(value = "getAllStudies")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<StudyDTO> getAllStudies(){
        return this.serverService.getAllStudies();
    }

    @PostMapping(value = "addNewSubject")
    @PreAuthorize("hasAuthority('STAFF')")
    SubjectDTO addNewSubject(@RequestBody SubjectDTO newSubjectData){
        return this.serverService.addNewSubject(newSubjectData);
    }

    @GetMapping(value = "getAllStudents/{studyId}/{year}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<StudentDTO> getAllStudentsBySY(@PathVariable String studyId, @PathVariable int year){
        return this.serverService.getAllStudentsByStudyIdAndAttendanceYear(studyId, year);
    }

    @GetMapping(value = "getAllSubjectsByProfessor/{professorId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<AttendanceHelperInstance> getSubjectByProfessorId(@PathVariable String professorId){
        return this.serverService.getSubjectsByProfessorId(professorId);
    }

    @GetMapping(value = "getSubject/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    SubjectDTO getSubjectById(@PathVariable String subjectId) {
        return this.serverService.getSubjectById(subjectId);
    }

    @GetMapping(value = "totalStudents/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    int totalStudentsBySubjectId(@PathVariable String subjectId){
        return this.serverService.totalStudentsBySubjectId(subjectId);
    }

    @GetMapping(value = "getAllLectures/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<ClassInstanceDTO> getAllLecturesBySubjectId(@PathVariable String subjectId){
        return this.serverService.getAllClassInstancesBySubjectId(false, subjectId);
    }

    @GetMapping(value = "getAllExercises/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<ClassInstanceDTO> getAllExercisesBySubjectId(@PathVariable String subjectId){
        return this.serverService.getAllClassInstancesBySubjectId(true, subjectId);
    }

    @GetMapping(value = "allStudentBySubjectId/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<StudentDTO> getAllStudentsBySubjectId(@PathVariable String subjectId){
        return this.serverService.getAllStudentsBySubjectId(subjectId);
    }

    @PatchMapping(value = "update/subject/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','STAFF')")
    SubjectDTO updateSubjectBySubjectId(@PathVariable String id, @RequestBody SubjectDTO newSubjectData){
        return this.serverService.updateSubjectBySubjectId(newSubjectData, id);
    }

    @GetMapping(value = "subjectIsInactiveById/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    boolean subjectIsInactiveById(@PathVariable String subjectId){
        return this.serverService.subjectIsInactiveById(subjectId);
    }

    @GetMapping(value = "getLastLecture/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    ClassInstanceDTO getLastLecture(@PathVariable String subjectId){
        return this.serverService.getLastClassInstanceBySubjectId(false, subjectId);
    }

    @PutMapping(value = "insert/lecture/{subjectId}/{start}/{end}")
    @PreAuthorize("hasAuthority('STAFF')")
    public void startNewLecture(@PathVariable String subjectId, @PathVariable String start, @PathVariable String end){
        this.serverService.startNewClassInstance(false, subjectId, start, end);
    }

    @GetMapping(value = "getLecture/{lectureId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    ClassInstanceDTO getLecture(@PathVariable String lectureId){
        return this.serverService.getClassInstance(false, lectureId);
    }

    @GetMapping(value = "startNewSubjectYear/{subjectId}")
    @PreAuthorize("hasAuthority('STAFF')")
    void startNewSubjectYear(@PathVariable String subjectId) {
        this.serverService.startNewSubjectYear(subjectId);
    }

    @GetMapping(value = "endCurrentSubjectYear/{subjectId}")
    @PreAuthorize("hasAuthority('STAFF')")
    void endCurrentSubjectYear(@PathVariable String subjectId) {
        this.serverService.endCurrentSubjectYear(subjectId);
    }

    @GetMapping(value = "getLastExercise/{subjectId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    ClassInstanceDTO getLastExercise(@PathVariable String subjectId){
        return this.serverService.getLastClassInstanceBySubjectId(true, subjectId);
    }

    @PutMapping(value = "insert/exercise/{subjectId}/{start}/{end}")
    @PreAuthorize("hasAuthority('STAFF')")
    public void startNewExercise(@PathVariable String subjectId, @PathVariable String start, @PathVariable String end){
        this.serverService.startNewClassInstance(true, subjectId, start, end);
    }

    @GetMapping(value = "getAllSubjectsByAssistant/{assistantId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<AttendanceHelperInstance> getSubjectsByAssistantId(@PathVariable String assistantId){
        return this.serverService.getSubjectsByAssistantId(assistantId);
    }

    @GetMapping(value = "getExercise/{exerciseId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    ClassInstanceDTO getExercise(@PathVariable String exerciseId){
        return this.serverService.getClassInstance(true, exerciseId);
    }

    @GetMapping(value = "getAllSubjects")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<AttendanceHelperInstance> getSubjects(){
        return this.serverService.getAllSubjects();
    }

    @GetMapping(value = "getAllStudents")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<StudentDTO> getAllStudents(){
        return this.serverService.getAllStudents();
    }

    @GetMapping(value = "getAllStaff")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    List<StaffDTO> getAllStaff(){
        return this.serverService.getAllStaff();
    }

    @GetMapping(value = "checkIfStaffHasSubjectAssigned/{staffId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    boolean checkIfStaffHasSubjectAssigned(@PathVariable String staffId){
        return this.serverService.checkIfStaffHasSubjectAssigned(staffId);
    }

    @DeleteMapping(value = "delete/staff/{staffId}/{isAssistant}")
    @PreAuthorize("hasAuthority('ADMIN')")
    void deleteStaff(@PathVariable String staffId, @PathVariable String isAssistant){
        this.serverService.deleteStaff(staffId,isAssistant.equals("1"));
    }

    @DeleteMapping(value = "delete/student/{studentId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    void deleteStudent(@PathVariable String studentId){
        this.serverService.deleteStudent(studentId);
    }

    @RequestMapping(path = "/invalidateSessionForUser/{username}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> invalidateUserSession(@PathVariable String username) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof UserDetails ud
                        && ud.getUsername().equals(username))
                .findFirst()
                .ifPresent(principal ->
                        sessionRegistry.getAllSessions(principal, false)
                                .forEach(SessionInformation::expireNow)
                );

        return ResponseEntity.ok().build();
    }
}
