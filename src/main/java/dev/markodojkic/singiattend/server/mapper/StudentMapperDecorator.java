package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Student;
import dev.markodojkic.singiattend.server.model.StudentDTO;
import dev.markodojkic.singiattend.server.repository.IStudyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentMapperDecorator implements StudentMapper {
    private StudentMapper studentMapper;
    private IStudyRepository studyRepository;
    private StudyMapper studyMapper;

    @Autowired
    public void setStudentMapper(@Qualifier("studentMapperImpl_") StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    @Autowired
    public void setStudyRepository(IStudyRepository studyRepository) {
        this.studyRepository = studyRepository;
    }

    @Autowired
    public void setStudyMapper(StudyMapper studyMapper) {
        this.studyMapper = studyMapper;
    }

    @Override
    public Student toEntity(StudentDTO studentDTO) {
        return studentMapper.toEntity(studentDTO);
    }

    @Override
    public StudentDTO toDTO(Student student) {
        StudentDTO studentDTO = studentMapper.toDTO(student);
        studentDTO.setStudy(studyMapper.toDTO(studyRepository.findById(studentDTO.getStudyId()).orElse(null)));
        return studentDTO;
    }

    @Override
    public List<StudentDTO> toDTOList(List<Student> students) {
        return students.stream().map(this::toDTO).toList();
    }
}
