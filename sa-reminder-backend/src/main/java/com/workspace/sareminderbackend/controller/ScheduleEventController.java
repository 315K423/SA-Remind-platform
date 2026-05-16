package com.workspace.sareminderbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.DeleteRequest;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleCalendarQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventAddRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.vo.ScheduleCalendarDayVO;
import com.workspace.sareminderbackend.model.vo.ScheduleEventSaveVO;
import com.workspace.sareminderbackend.model.vo.ScheduleEventVO;
import com.workspace.sareminderbackend.service.ScheduleEventService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
public class ScheduleEventController {

    @Resource
    private ScheduleEventService scheduleEventService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    @AuthCheck
    public BaseResponse<ScheduleEventSaveVO> add(@RequestBody ScheduleEventAddRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleEventService.addScheduleEvent(request, loginUser));
    }

    @GetMapping("/get")
    @AuthCheck
    public BaseResponse<ScheduleEventVO> getById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ScheduleEvent scheduleEvent = scheduleEventService.getById(id);
        ThrowUtils.throwIf(scheduleEvent == null, ErrorCode.NOT_FOUND_ERROR);
        ScheduleEventQueryRequest queryRequest = new ScheduleEventQueryRequest();
        queryRequest.setId(id);
        List<ScheduleEvent> list = scheduleEventService.list(scheduleEventService.getQueryWrapper(queryRequest, loginUser));
        ThrowUtils.throwIf(list.isEmpty(), ErrorCode.NO_AUTH_ERROR);
        return ResultUtils.success(scheduleEventService.getScheduleEventVO(scheduleEvent));
    }

    @PostMapping("/update")
    @AuthCheck
    public BaseResponse<ScheduleEventSaveVO> update(@RequestBody ScheduleEventUpdateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleEventService.updateScheduleEvent(request, loginUser));
    }

    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleEventService.deleteScheduleEvent(deleteRequest.getId(), loginUser));
    }

    @PostMapping("/list/page/vo")
    @AuthCheck
    public BaseResponse<Page<ScheduleEventVO>> listPage(@RequestBody ScheduleEventQueryRequest queryRequest,
                                                        HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<ScheduleEvent> page = scheduleEventService.page(Page.of(pageNum, pageSize),
                scheduleEventService.getQueryWrapper(queryRequest, loginUser));
        Page<ScheduleEventVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(scheduleEventService.getScheduleEventVOList(page.getRecords()));
        return ResultUtils.success(voPage);
    }

    /**
     * 查询当前用户当月日程
     *
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/my/month")
    @AuthCheck
    public BaseResponse<List<ScheduleEventVO>> listMyMonth(@RequestBody ScheduleCalendarQueryRequest request,
                                                           HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getYear() == null || request.getMonth() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleEventService.listMyMonthSchedule(request.getYear(), request.getMonth(), loginUser));
    }

    /**
     * 查询当前用户某一天日程
     *
     * @param date
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/my/day")
    @AuthCheck
    public BaseResponse<ScheduleCalendarDayVO> getMyDay(@RequestParam("date") String date,
                                                        HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleEventService.getMyDaySchedule(LocalDate.parse(date), loginUser));
    }

    @PostMapping("/admin/cancel")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminCancel(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        ScheduleEvent update = new ScheduleEvent();
        update.setId(deleteRequest.getId());
        update.setStatus("cancelled");
        return ResultUtils.success(scheduleEventService.updateById(update));
    }
}
