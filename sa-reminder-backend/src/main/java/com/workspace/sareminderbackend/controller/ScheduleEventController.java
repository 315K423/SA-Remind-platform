package com.workspace.sareminderbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.DeleteRequest;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventAddRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventUpdateRequest;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.ScheduleEventVO;
import com.workspace.sareminderbackend.service.ScheduleEventService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日程管理 控制层。
 */
@RestController
@RequestMapping("/schedule")
public class ScheduleEventController {

    @Resource
    private ScheduleEventService scheduleEventService;

    @Resource
    private UserService userService;

    /**
     * 创建日程（登录即可）
     */
    @PostMapping("/add")
    @AuthCheck
    public BaseResponse<Long> add(@RequestBody ScheduleEventAddRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        long id = scheduleEventService.addScheduleEvent(request, loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 根据 id 获取日程（登录即可；内部会做权限收敛）
     */
    @GetMapping("/get")
    @AuthCheck
    public BaseResponse<ScheduleEventVO> getById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ScheduleEvent scheduleEvent = scheduleEventService.getById(id);
        ThrowUtils.throwIf(scheduleEvent == null, ErrorCode.NOT_FOUND_ERROR);

        // 权限：普通用户只能获取自己创建/参与的
        // 复用 list 的权限收敛：如果查不到，按无权限处理
        ScheduleEventQueryRequest queryRequest = new ScheduleEventQueryRequest();
        queryRequest.setId(id);
        List<ScheduleEvent> list = scheduleEventService.list(scheduleEventService.getQueryWrapper(queryRequest, loginUser));
        ThrowUtils.throwIf(list.isEmpty(), ErrorCode.NO_AUTH_ERROR);

        return ResultUtils.success(scheduleEventService.getScheduleEventVO(scheduleEvent));
    }

    /**
     * 更新日程（管理员或创建人）
     */
    @PostMapping("/update")
    @AuthCheck
    public BaseResponse<Boolean> update(@RequestBody ScheduleEventUpdateRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean ok = scheduleEventService.updateScheduleEvent(request, loginUser);
        return ResultUtils.success(ok);
    }

    /**
     * 删除日程（管理员或创建人）
     */
    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean ok = scheduleEventService.deleteScheduleEvent(deleteRequest.getId(), loginUser);
        return ResultUtils.success(ok);
    }

    /**
     * 分页查询日程（登录即可）
     */
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
     * 管理员：强制取消某日程（演示“管理员端日程管理”最小闭环）
     */
    @PostMapping("/admin/cancel")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminCancel(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        ScheduleEvent update = new ScheduleEvent();
        update.setId(deleteRequest.getId());
        update.setStatus("cancelled");
        boolean ok = scheduleEventService.updateById(update);
        return ResultUtils.success(ok);
    }
}
