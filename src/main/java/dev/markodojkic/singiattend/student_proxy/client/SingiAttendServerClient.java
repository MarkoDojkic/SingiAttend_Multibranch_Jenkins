package dev.markodojkic.singiattend.student_proxy.client;

import dev.markodojkic.singiattend.student_proxy.config.FeignConfig;
import dev.markodojkic.singiattend.student_proxy.model.AttendanceDataInstance;
import dev.markodojkic.singiattend.student_proxy.model.CourseDataInstance;
import dev.markodojkic.singiattend.student_proxy.model.StudentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
    name = "${feign_client.name}",
    url = "${feign_client.url}",
    path = "${feign_client.path}",
    contextId = "${feign_client.context-id}",
    configuration = FeignConfig.class
)
public interface SingiAttendServerClient {

    @GetMapping("/api/v1/csrfLogin")
    ResponseEntity<DefaultCsrfToken> csrfLogin();

    @PostMapping("/api/v1/insert/student")
    StudentDTO addNewStudent(@RequestBody StudentDTO newStudent);

    @GetMapping("/api/v1/getCourseData/{index}")
    List<CourseDataInstance> getCourseData(@PathVariable("index") String index);

    @GetMapping("/api/v1/getAttendanceData/{index}")
    List<AttendanceDataInstance> getAttendanceData(@PathVariable("index") String index);

    @PostMapping("/api/v1/checkPassword/student/{index}")
    String checkPasswordStudent(@PathVariable("index") String index, @RequestBody String password);

    @GetMapping("/api/v1/getStudentName/{index}")
    String getStudentName(@PathVariable("index") String index);

    @GetMapping("/api/v1/recordAttendance/{index}/{id}/{isExercise}")
    String recordAttendance(
        @PathVariable("index") String index,
        @PathVariable("id") String id,
        @PathVariable("isExercise") boolean isExercise
    );

    @PostMapping("/api/v1/csrfLogout")
    void csrfLogout();
}