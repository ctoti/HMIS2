package com.pha.hr.staff;

import org.mapstruct.Mapper;

@Mapper
public interface StaffMapper {

    StaffResponse toResponse(Staff staff);
}
