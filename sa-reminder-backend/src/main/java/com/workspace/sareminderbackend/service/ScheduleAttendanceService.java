package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceCheckInRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceCheckInVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceVO;

public interface ScheduleAttendanceService {

    ScheduleAttendanceCheckInVO checkInByTask(ScheduleAttendanceCheckInRequest request, User loginUser);

    Page<ScheduleAttendanceVO> listAttendancePage(ScheduleAttendanceQueryRequest request);

    boolean adminUpdateAttendanceStatus(ScheduleAttendanceUpdateRequest request);
}
