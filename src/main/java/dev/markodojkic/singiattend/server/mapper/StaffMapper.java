package dev.markodojkic.singiattend.server.mapper;

import dev.markodojkic.singiattend.server.entity.Staff;
import dev.markodojkic.singiattend.server.model.StaffDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface StaffMapper {
    Staff toEntity(final StaffDTO staffDTO);
    
    StaffDTO toDTO(final Staff staff);  // Includes password
    
    @Mapping(target = "passwordHash", ignore = true)
    StaffDTO toDTOWithoutPassword(final Staff staff);  // Excludes password
    
    default List<StaffDTO> toDTOList(final List<Staff> staffList) {
        return staffList.stream()
            .map(this::toDTOWithoutPassword)
            .toList();
    }
}
