package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Subject;
import dev.markodojkic.singiattend.server.model.SubjectDTO;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface SubjectMapper {
    Subject toEntity(final SubjectDTO subjectDTO);
    SubjectDTO toDTO(final Subject subject);
    List<SubjectDTO> toDTOList(final List<Subject> subjects);
}
