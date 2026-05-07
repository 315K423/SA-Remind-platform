package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceCheckInRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceCheckInVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceRateVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceVO;

public interface ScheduleAttendanceService {

    ScheduleAttendanceCheckInVO checkInByTask(ScheduleAttendanceCheckInRequest request, User loginUser);

    Page<ScheduleAttendanceVO> listAttendancePage(ScheduleAttendanceQueryRequest request);

    boolean adminUpdateAttendanceStatus(ScheduleAttendanceUpdateRequest request);

    /**
     * 分页统计每个考勤事项的考勤率。
     */
    Page<ScheduleAttendanceRateVO> listAttendanceRatePage(ScheduleAttendanceQueryRequest request);
}
