package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.ClassInstance;
import dev.markodojkic.singiattend.server.model.ClassInstanceDTO;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface ClassInstanceMapper {
    ClassInstance toEntity(final ClassInstanceDTO classInstanceDTO);
    ClassInstanceDTO toDTO(final ClassInstance classInstance);
    List<ClassInstanceDTO> toDTOList(final List<ClassInstance> classInstances);
}
