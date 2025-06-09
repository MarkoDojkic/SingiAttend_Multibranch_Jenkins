package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Student;
import dev.markodojkic.singiattend.server.model.StudentDTO;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
@DecoratedWith(StudentMapperDecorator.class)
public interface StudentMapper {
    Student toEntity(final StudentDTO studentDTO);
    StudentDTO toDTO(final Student student);
    List<StudentDTO> toDTOList(final List<Student> students);
}
