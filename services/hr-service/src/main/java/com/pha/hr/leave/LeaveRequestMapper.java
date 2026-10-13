package com.pha.hr.leave;

import org.mapstruct.Mapper;

@Mapper
public interface LeaveRequestMapper {

    LeaveRequestResponse toResponse(LeaveRequest leaveRequest);
}
