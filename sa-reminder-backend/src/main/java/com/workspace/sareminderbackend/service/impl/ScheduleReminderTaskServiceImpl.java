package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.ScheduleEventMapper;
import com.workspace.sareminderbackend.mapper.ScheduleParticipantMapper;
import com.workspace.sareminderbackend.mapper.ScheduleReminderTaskMapper;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleParticipant;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderPopupVO;
import com.workspace.sareminderbackend.service.ScheduleReminderTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ScheduleReminderTaskServiceImpl extends ServiceImpl<ScheduleReminderTaskMapper, ScheduleReminderTask>
        implements ScheduleReminderTaskService {

    public static final String TASK_SENT = "sent";
    public static final String TASK_READ = "read";
    public static final String TASK_EXPIRED = "expired";

    public static final String ATTENDANCE_CHECKED_IN = "checked_in";

    @Resource
    private ScheduleParticipantMapper scheduleParticipantMapper;

    @Resource
    private ScheduleEventMapper scheduleEventMapper;

    @Override
    public List<ScheduleReminderPopupVO> listPopupTasks(User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("taskStatus", TASK_SENT)
                .eq("isDelete", 0)
                .orderBy("plannedRemindTime", true);
        List<ScheduleReminderTask> taskList = this.list(wrapper);
        if (CollUtil.isEmpty(taskList)) {
            return new ArrayList<>();
        }
        return taskList.stream().map(task -> {
            ScheduleReminderPopupVO vo = new ScheduleReminderPopupVO();
            BeanUtil.copyProperties(task, vo);
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(task.getScheduleId());
            if (scheduleEvent != null) {
                vo.setScheduleType(scheduleEvent.getScheduleType());
                boolean attendanceCheckRequired = isAttendanceCheckRequired(scheduleEvent);
                vo.setAttendanceCheckRequired(attendanceCheckRequired);
                vo.setCheckInAddress(scheduleEvent.getCheckInAddress());
                vo.setCheckInRadiusMeters(scheduleEvent.getCheckInRadiusMeters());
            } else {
                vo.setAttendanceCheckRequired(false);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean readPopupTask(long taskId, User loginUser) {
        if (taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        ScheduleReminderTask task = this.getById(taskId);
        if (task == null || (task.getIsDelete() != null && task.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!Objects.equals(task.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (!TASK_SENT.equals(task.getTaskStatus())) {
            return true;
        }

        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(task.getScheduleId());
        if (isAttendanceCheckRequired(scheduleEvent)) {
            ScheduleParticipant participant = scheduleParticipantMapper.selectOneByQuery(QueryWrapper.create()
                    .eq("scheduleId", task.getScheduleId())
                    .eq("userId", loginUser.getId())
                    .eq("isDelete", 0));
            if (participant == null || !ATTENDANCE_CHECKED_IN.equals(participant.getAttendanceStatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先完成定位签到");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ScheduleReminderTask update = new ScheduleReminderTask();
        update.setId(taskId);
        update.setTaskStatus(TASK_READ);
        update.setReadTime(now);
        update.setUpdateTime(now);
        boolean ok = this.mapper.update(update) > 0;

        List<ScheduleReminderTask> sameRuleTasks = this.list(QueryWrapper.create()
                .eq("ruleId", task.getRuleId())
                .eq("isDelete", 0));
        for (ScheduleReminderTask item : sameRuleTasks) {
            if (Objects.equals(item.getId(), taskId)) {
                continue;
            }
            if (TASK_READ.equals(item.getTaskStatus()) || TASK_EXPIRED.equals(item.getTaskStatus())) {
                continue;
            }
            ScheduleReminderTask expireUpdate = new ScheduleReminderTask();
            expireUpdate.setId(item.getId());
            expireUpdate.setTaskStatus(TASK_EXPIRED);
            expireUpdate.setUpdateTime(now);
            this.mapper.update(expireUpdate);
        }

        ScheduleParticipant participant = scheduleParticipantMapper.selectOneByQuery(QueryWrapper.create()
                .eq("scheduleId", task.getScheduleId()).eq("userId", loginUser.getId()).eq("isDelete", 0));
        if (participant != null) {
            ScheduleParticipant updateParticipant = new ScheduleParticipant();
            updateParticipant.setId(participant.getId());
            updateParticipant.setResponseStatus("accepted");
            updateParticipant.setUpdateTime(now);
            scheduleParticipantMapper.update(updateParticipant);
        }
        return ok;
    }

    @Override
    public boolean readAllPopupTasks(List<Long> taskIdList, User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("taskStatus", TASK_SENT)
                .eq("isDelete", 0);
        if (CollUtil.isNotEmpty(taskIdList)) {
            wrapper.in("id", taskIdList);
        }
        List<ScheduleReminderTask> taskList = this.list(wrapper);
        if (CollUtil.isEmpty(taskList)) {
            return true;
        }
        for (ScheduleReminderTask task : taskList) {
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(task.getScheduleId());
            if (isAttendanceCheckRequired(scheduleEvent)) {
                continue;
            }
            readPopupTask(task.getId(), loginUser);
        }
        return true;
    }

    private boolean isAttendanceCheckRequired(ScheduleEvent scheduleEvent) {
        return scheduleEvent != null
                && "attendance".equalsIgnoreCase(scheduleEvent.getScheduleType())
                && Objects.equals(scheduleEvent.getCheckInEnabled(), 1)
                && scheduleEvent.getCheckInLatitude() != null
                && scheduleEvent.getCheckInLongitude() != null;
    }
}
