package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Subject;
import dev.markodojkic.singiattend.server.model.StudentDTO;
import dev.markodojkic.singiattend.server.model.SubjectDTO;
import dev.markodojkic.singiattend.server.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectMapperDecorator implements SubjectMapper {

    private SubjectMapper subjectMapper;
    private ServerService serverService;

    @Autowired
    @Lazy
    public void setServerService(ServerService serverService) {
        this.serverService = serverService;
    }

    @Autowired
    public void setSubjectMapper(@Qualifier("subjectMapperImpl_") SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    @Override
    public Subject toEntity(SubjectDTO subjectDTO) {
        return subjectMapper.toEntity(subjectDTO);
    }

    @Override
    public SubjectDTO toDTO(Subject subject) {
        SubjectDTO dto = subjectMapper.toDTO(subject);

        List<String> ids = subject.getEnrolledStudyIds().stream()
                .flatMap(studyId -> {
                    String[] parts = studyId.split("_");
                    return serverService.getAllStudentsBySubjectIdAndAttendanceYear(parts[0], Integer.parseInt(parts[1])).stream();
                })
                .map(StudentDTO::getId).toList();

        dto.setEnrolledStudentIds(ids);
        return dto;
    }

    @Override
    public List<SubjectDTO> toDTOList(List<Subject> subjects) {
        return subjects.stream().map(this::toDTO).toList();
    }
}
