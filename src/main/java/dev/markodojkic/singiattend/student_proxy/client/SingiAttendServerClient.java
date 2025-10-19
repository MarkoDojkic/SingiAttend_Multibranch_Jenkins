package dev.markodojkic.singiattend.student_proxy.client;

import dev.markodojkic.singiattend.student_proxy.config.FeignConfig;
import dev.markodojkic.singiattend.student_proxy.model.AttendanceDataInstance;
import dev.markodojkic.singiattend.student_proxy.model.CourseDataInstance;
import dev.markodojkic.singiattend.student_proxy.model.StudentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
    name = "${feign.singiattend.server_client}",
    configuration = FeignConfig.class
)
public interface SingiAttendServerClient {

    @GetMapping("/csrfLogin")
    ResponseEntity<CsrfToken> csrfLogin();

    @PostMapping("/api/insert/student")
    StudentDTO addNewStudent(@RequestBody StudentDTO newStudent);

    @GetMapping("/api/getCourseData/{index}")
    List<CourseDataInstance> getCourseData(@PathVariable("index") String index);

    @GetMapping("/api/getAttendanceData/{index}")
    List<AttendanceDataInstance> getAttendanceData(@PathVariable("index") String index);

    @PostMapping("/api/checkPassword/student/{index}")
    String checkPasswordStudent(@PathVariable("index") String index, @RequestBody String password);

    @GetMapping("/api/getStudentName/{index}")
    String getStudentName(@PathVariable("index") String index);

    @GetMapping("/api/recordAttendance/{index}/{id}/{isExercise}")
    String recordAttendance(
        @PathVariable("index") String index,
        @PathVariable("id") String id,
        @PathVariable("isExercise") boolean isExercise
    );

    @PostMapping("/api/csrfLogout")
    void csrfLogout();
}