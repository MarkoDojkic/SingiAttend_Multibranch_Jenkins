package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Staff;
import dev.markodojkic.singiattend.server.model.StaffDTO;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface StaffMapper {
    Staff toEntity(final StaffDTO staffDTO);
    StaffDTO toDTO(final Staff staff);
    List<StaffDTO> toDTOList(final List<Staff> staffList);
}
