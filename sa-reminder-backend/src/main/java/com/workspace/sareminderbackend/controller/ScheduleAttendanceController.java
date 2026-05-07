package com.workspace.sareminderbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceCheckInRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceCheckInVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceRateVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceVO;
import com.workspace.sareminderbackend.service.ScheduleAttendanceService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule/attendance")
public class ScheduleAttendanceController {

    @Resource
    private ScheduleAttendanceService scheduleAttendanceService;

    @Resource
    private UserService userService;

    @PostMapping("/checkIn")
    @AuthCheck
    public BaseResponse<ScheduleAttendanceCheckInVO> checkIn(@RequestBody ScheduleAttendanceCheckInRequest request,
                                                             HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleAttendanceService.checkInByTask(request, loginUser));
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ScheduleAttendanceVO>> listAttendancePage(@RequestBody ScheduleAttendanceQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(scheduleAttendanceService.listAttendancePage(request));
    }

    @PostMapping("/admin/updateStatus")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAttendanceStatus(@RequestBody ScheduleAttendanceUpdateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(scheduleAttendanceService.adminUpdateAttendanceStatus(request));
    }

    /**
     * 分页查询考勤率统计。
     * 用于后台“考勤管理 / 考勤率统计”子菜单。
     */
    @PostMapping("/admin/stat/rate/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ScheduleAttendanceRateVO>> listAttendanceRatePage(@RequestBody ScheduleAttendanceQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(scheduleAttendanceService.listAttendanceRatePage(request));
    }
}
