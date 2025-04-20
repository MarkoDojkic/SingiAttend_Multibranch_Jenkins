package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Study;
import dev.markodojkic.singiattend.server.model.StudyDTO;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface StudyMapper {
    Study toEntity(final StudyDTO studyDTO);
    StudyDTO toDTO(final Study study);
    List<StudyDTO> toDTOList(final List<Study> studies);
}
