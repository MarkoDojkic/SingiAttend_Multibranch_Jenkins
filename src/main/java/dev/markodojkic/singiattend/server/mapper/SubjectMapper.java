package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Subject;
import dev.markodojkic.singiattend.server.model.SubjectDTO;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
@DecoratedWith(SubjectMapperDecorator.class)
public interface SubjectMapper {
    Subject toEntity(final SubjectDTO subjectDTO);
    @Mapping(target = "enrolledStudentIds", ignore = true)
    SubjectDTO toDTO(final Subject subject);
    List<SubjectDTO> toDTOList(final List<Subject> subjects);
}
