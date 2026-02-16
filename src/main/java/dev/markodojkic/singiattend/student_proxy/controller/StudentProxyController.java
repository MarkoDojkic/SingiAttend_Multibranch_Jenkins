package dev.markodojkic.singiattend.student_proxy.controller;

import dev.markodojkic.singiattend.student_proxy.client.SingiAttendServerClient;
import dev.markodojkic.singiattend.student_proxy.model.AttendanceDataInstance;
import dev.markodojkic.singiattend.student_proxy.model.CourseDataInstance;
import dev.markodojkic.singiattend.student_proxy.model.StudentDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StudentProxyController {
    private final SingiAttendServerClient backendClient;
    private final Logger logger = LoggerFactory.getLogger(StudentProxyController.class);

    public StudentProxyController(SingiAttendServerClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping("/csrfLogin")
    public ResponseEntity<DefaultCsrfToken> csrfLogin(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        logger.info("CSRF login requested from IP: {}", clientIp);
        return backendClient.csrfLogin();
    }

    @PostMapping("/insert/student")
    public StudentDTO addNewStudent(@RequestBody StudentDTO student,
                                    @RequestHeader(value = "X-Client-Index", required = false) String index,
                                    @RequestHeader(value = "X-Client-Device", required = false) String device,
                                    HttpServletRequest request) {

        logCall(request, index, device, "/insert/student");
        return backendClient.addNewStudent(student);
    }

    @GetMapping("/getCourseData/{index}")
    public List<CourseDataInstance> getCourseData(@PathVariable String index,
                                                  @RequestHeader(value = "X-Client-Device", required = false) String device,
                                                  HttpServletRequest request) {
        logCall(request, index, device, "/getCourseData");
        return backendClient.getCourseData(index);
    }

    @GetMapping("/getAttendanceData/{index}")
    public List<AttendanceDataInstance> getAttendanceData(@PathVariable String index,
                                                          @RequestHeader(value = "X-Client-Device", required = false) String device,
                                                          HttpServletRequest request) {
        logCall(request, index, device, "/getAttendanceData");
        return backendClient.getAttendanceData(index);
    }

    @PostMapping("/checkPassword/student/{index}")
    public String checkPassword(@PathVariable String index,
                                @RequestBody String password,
                                @RequestHeader(value = "X-Client-Device", required = false) String device,
                                HttpServletRequest request) {
        logCall(request, index, device, "/checkPassword/student");
        return backendClient.checkPasswordStudent(index, password);
    }

    @GetMapping("/getStudentName/{index}")
    public String getStudentName(@PathVariable String index,
                                 @RequestHeader(value = "X-Client-Device", required = false) String device,
                                 HttpServletRequest request) {
        logCall(request, index, device, "/getStudentName");
        return backendClient.getStudentName(index);
    }

    @GetMapping("/recordAttendance/{index}/{id}/{isExercise}")
    public String recordAttendance(@PathVariable String index,
                                   @PathVariable String id,
                                   @PathVariable boolean isExercise,
                                   @RequestHeader(value = "X-Client-Device", required = false) String device,
                                   HttpServletRequest request) {
        logCall(request, index, device, "/recordAttendance");
        return backendClient.recordAttendance(index, id, isExercise);
    }

    private void logCall(HttpServletRequest request, String index, String device, String endpoint) {
        String clientIp = getClientIp(request);
        logger.info("Proxy call to '{}' from IP: {}, index: {}, device: {}",
                endpoint, clientIp, index, device);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}