package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.ScheduleReminderTaskMapper;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderPopupVO;
import com.workspace.sareminderbackend.service.ScheduleReminderTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 日程提醒任务 服务实现。
 */
@Service
public class ScheduleReminderTaskServiceImpl extends ServiceImpl<ScheduleReminderTaskMapper, ScheduleReminderTask>
        implements ScheduleReminderTaskService {

    public static final String TASK_PENDING = "pending";
    public static final String TASK_SENT = "sent";
    public static final String TASK_READ = "read";

    @Override
    public List<ScheduleReminderPopupVO> listPopupTasks(User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("taskStatus", TASK_SENT)
                .orderBy("plannedRemindTime", true);
        List<ScheduleReminderTask> taskList = this.list(wrapper);
        if (CollUtil.isEmpty(taskList)) {
            return new ArrayList<>();
        }
        return taskList.stream().map(task -> {
            ScheduleReminderPopupVO vo = new ScheduleReminderPopupVO();
            BeanUtil.copyProperties(task, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean readPopupTask(long taskId, User loginUser) {
        if (taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ScheduleReminderTask task = this.getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!Objects.equals(task.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (!TASK_SENT.equals(task.getTaskStatus())) {
            return true;
        }
        ScheduleReminderTask update = new ScheduleReminderTask();
        update.setId(taskId);
        update.setTaskStatus(TASK_READ);
        update.setReadTime(LocalDateTime.now());
        return this.updateById(update);
    }

    @Override
    public boolean readAllPopupTasks(List<Long> taskIdList, User loginUser) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("taskStatus", TASK_SENT);
        if (CollUtil.isNotEmpty(taskIdList)) {
            wrapper.in("id", taskIdList);
        }
        List<ScheduleReminderTask> taskList = this.list(wrapper);
        if (CollUtil.isEmpty(taskList)) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ScheduleReminderTask task : taskList) {
            ScheduleReminderTask update = new ScheduleReminderTask();
            update.setId(task.getId());
            update.setTaskStatus(TASK_READ);
            update.setReadTime(now);
            this.updateById(update);
        }
        return true;
    }
}
