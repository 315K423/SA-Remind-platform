package com.workspace.sareminderbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.DeleteRequest;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderPopupReadAllRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderRuleQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderRuleSaveRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderRule;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderPopupVO;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderRuleVO;
import com.workspace.sareminderbackend.service.ScheduleReminderRuleService;
import com.workspace.sareminderbackend.service.ScheduleReminderTaskService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedule/reminder")
public class ScheduleReminderController {

    @Resource
    private ScheduleReminderRuleService scheduleReminderRuleService;

    @Resource
    private ScheduleReminderTaskService scheduleReminderTaskService;

    @Resource
    private UserService userService;

    @PostMapping("/rule/save")
    @AuthCheck
    public BaseResponse<Long> saveRule(@RequestBody ScheduleReminderRuleSaveRequest request,
                                       HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        long id = scheduleReminderRuleService.saveOrUpdateRule(request, loginUser);
        return ResultUtils.success(id);
    }

    /**
     *  根据日程ID获取提醒规则
     *
     * @param scheduleId
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/rule/get")
    @AuthCheck
    public BaseResponse<ScheduleReminderRuleVO> getRuleByScheduleId(long scheduleId,
                                                                    HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(scheduleId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<ScheduleReminderRule> list = scheduleReminderRuleService.list(
                scheduleReminderRuleService.getQueryWrapper(buildGetRequest(scheduleId), loginUser)
        );
        if (list.isEmpty()) {
            return ResultUtils.success(null);
        }
        return ResultUtils.success(scheduleReminderRuleService.getScheduleReminderRuleVO(list.get(0)));
    }

    @PostMapping("/rule/delete")
    @AuthCheck
    public BaseResponse<Boolean> deleteRule(@RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = scheduleReminderRuleService.deleteRule(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     *  分页查询列表
     *
     * @param queryRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/rule/list/page/vo")
    @AuthCheck
    public BaseResponse<Page<ScheduleReminderRuleVO>> listRuleByPage(@RequestBody ScheduleReminderRuleQueryRequest queryRequest,
                                                                     HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<ScheduleReminderRule> page = scheduleReminderRuleService.page(
                Page.of(pageNum, pageSize),
                scheduleReminderRuleService.getQueryWrapper(queryRequest, loginUser)
        );
        Page<ScheduleReminderRuleVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(scheduleReminderRuleService.getScheduleReminderRuleVOList(page.getRecords()));
        return ResultUtils.success(voPage);
    }

    /**
     *  获取当前用户未读的提醒任务列表
     *
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/popup/list")
    @AuthCheck
    public BaseResponse<List<ScheduleReminderPopupVO>> listPopupTasks(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleReminderTaskService.listPopupTasks(loginUser));
    }

    /**
     *  读取单条弹窗任务
     *
     * @param deleteRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/popup/read")
    @AuthCheck
    public BaseResponse<Boolean> readPopup(@RequestBody DeleteRequest deleteRequest,
                                           HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(scheduleReminderTaskService.readPopupTask(deleteRequest.getId(), loginUser));
    }

    /**
     *  读取所有弹窗任务
     *
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/popup/read/all")
    @AuthCheck
    public BaseResponse<Boolean> readAllPopup(@RequestBody(required = false) ScheduleReminderPopupReadAllRequest request,
                                              HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<Long> taskIdList = request == null ? null : request.getTaskIdList();
        return ResultUtils.success(scheduleReminderTaskService.readAllPopupTasks(taskIdList, loginUser));
    }

    /**
     * 仅管理员允许手动触发一次全量扫描
     */
    @PostMapping("/admin/scanNow")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> scanNow() {
        scheduleReminderRuleService.scanAndDispatchOnce();
        return ResultUtils.success(true);
    }

    private ScheduleReminderRuleQueryRequest buildGetRequest(long scheduleId) {
        ScheduleReminderRuleQueryRequest request = new ScheduleReminderRuleQueryRequest();
        request.setScheduleId(scheduleId);
        request.setPageNum(1);
        request.setPageSize(1);
        return request;
    }
}