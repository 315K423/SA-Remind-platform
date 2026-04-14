package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.ScheduleEventMapper;
import com.workspace.sareminderbackend.mapper.ScheduleParticipantMapper;
import com.workspace.sareminderbackend.mapper.ScheduleReminderRuleMapper;
import com.workspace.sareminderbackend.mapper.ScheduleReminderTaskMapper;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderRuleQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderRuleSaveRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleParticipant;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderRule;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderRuleVO;
import com.workspace.sareminderbackend.service.ScheduleReminderRuleService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ScheduleReminderRuleServiceImpl extends ServiceImpl<ScheduleReminderRuleMapper, ScheduleReminderRule>
        implements ScheduleReminderRuleService {

    public static final String STATUS_ENABLED = "enabled";
    public static final String STATUS_DISABLED = "disabled";

    public static final String TASK_PENDING = "pending";
    public static final String TASK_SENT = "sent";
    public static final String TASK_READ = "read";
    public static final String TASK_EXPIRED = "expired";

    @Resource
    private ScheduleEventMapper scheduleEventMapper;

    @Resource
    private ScheduleParticipantMapper scheduleParticipantMapper;

    @Resource
    private ScheduleReminderTaskMapper scheduleReminderTaskMapper;

    @Resource
    private UserService userService;

    @Override
    public long saveOrUpdateRule(ScheduleReminderRuleSaveRequest request, User loginUser) {
        if (request == null || request.getScheduleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Integer remindOffsetMinutes = request.getRemindOffsetMinutes();
        Integer repeatIntervalMinutes = request.getRepeatIntervalMinutes();

        if (remindOffsetMinutes == null || remindOffsetMinutes < 0 || remindOffsetMinutes > 30 * 24 * 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提醒时间范围不合法");
        }
        if (repeatIntervalMinutes == null || repeatIntervalMinutes < 1 || repeatIntervalMinutes > 24 * 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复间隔不合法");
        }

        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(request.getScheduleId());
        if (scheduleEvent == null || (scheduleEvent.getIsDelete() != null && scheduleEvent.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "日程不存在");
        }

        checkScheduleAccess(scheduleEvent, loginUser);

        if (scheduleEvent.getStartTime() == null || !scheduleEvent.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已开始或已过期的日程不允许再设置提醒");
        }

        ScheduleReminderRule exist = this.mapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("scheduleId", request.getScheduleId())
                        .eq("userId", loginUser.getId())
                        .eq("isDelete", 0)
        );

        LocalDateTime now = LocalDateTime.now();

        if (exist == null) {
            ScheduleReminderRule rule = new ScheduleReminderRule();
            rule.setScheduleId(request.getScheduleId());
            rule.setUserId(loginUser.getId());
            rule.setRemindOffsetMinutes(remindOffsetMinutes);
            rule.setRepeatCount(request.getRepeatCount() == null ? 0 : request.getRepeatCount());
            rule.setRepeatIntervalMinutes(repeatIntervalMinutes);
            rule.setPopupEnabled(request.getPopupEnabled() == null ? 1 : request.getPopupEnabled());
            rule.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
            rule.setCreateTime(now);
            rule.setUpdateTime(now);
            rule.setIsDelete(0);

            boolean saved = this.save(rule);
            if (!saved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存提醒策略失败");
            }

            rebuildFirstTask(rule, scheduleEvent);
            return rule.getId();
        }

        ScheduleReminderRule update = new ScheduleReminderRule();
        update.setId(exist.getId());
        update.setRemindOffsetMinutes(remindOffsetMinutes);
        update.setRepeatCount(request.getRepeatCount() == null ? exist.getRepeatCount() : request.getRepeatCount());
        update.setRepeatIntervalMinutes(repeatIntervalMinutes);
        update.setPopupEnabled(request.getPopupEnabled() == null ? exist.getPopupEnabled() : request.getPopupEnabled());
        update.setStatus(request.getStatus() == null ? exist.getStatus() : request.getStatus());
        update.setUpdateTime(now);

        boolean ok = this.updateById(update);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提醒策略失败");
        }

        ScheduleReminderRule latest = this.getById(exist.getId());
        rebuildFirstTask(latest, scheduleEvent);
        return latest.getId();
    }

    @Override
    public boolean deleteRule(long id, User loginUser) {
        ScheduleReminderRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!userService.isAdmin(loginUser) && !Objects.equals(rule.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        scheduleReminderTaskMapper.deleteByQuery(QueryWrapper.create().eq("ruleId", id));
        return this.removeById(id);
    }

    @Override
    public QueryWrapper getQueryWrapper(ScheduleReminderRuleQueryRequest request, User loginUser) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("id", request.getId())
                .eq("scheduleId", request.getScheduleId())
                .eq("status", request.getStatus())
                .eq("isDelete", 0);

        if (userService.isAdmin(loginUser)) {
            wrapper.eq("userId", request.getUserId());
        } else {
            wrapper.eq("userId", loginUser.getId());
        }

        return wrapper.orderBy(request.getSortField(), "ascend".equals(request.getSortOrder()));
    }

    @Override
    public ScheduleReminderRuleVO getScheduleReminderRuleVO(ScheduleReminderRule rule) {
        if (rule == null) {
            return null;
        }
        ScheduleReminderRuleVO vo = new ScheduleReminderRuleVO();
        BeanUtil.copyProperties(rule, vo);
        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(rule.getScheduleId());
        if (scheduleEvent != null) {
            vo.setScheduleTitle(scheduleEvent.getTitle());
            vo.setScheduleStartTime(scheduleEvent.getStartTime());
        }
        return vo;
    }

    @Override
    public List<ScheduleReminderRuleVO> getScheduleReminderRuleVOList(List<ScheduleReminderRule> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(this::getScheduleReminderRuleVO).collect(Collectors.toList());
    }

    @Override
    public void scanAndDispatchOnce() {
        LocalDateTime now = LocalDateTime.now();

        List<ScheduleReminderRule> ruleList = this.list(
                QueryWrapper.create()
                        .eq("status", STATUS_ENABLED)
                        .eq("popupEnabled", 1)
                        .eq("isDelete", 0)
        );

        for (ScheduleReminderRule rule : ruleList) {
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(rule.getScheduleId());

            // 日程不存在、被删除、没有开始时间、已开始/已过期 -> 全部过期
            if (scheduleEvent == null
                    || (scheduleEvent.getIsDelete() != null && scheduleEvent.getIsDelete() == 1)
                    || scheduleEvent.getStartTime() == null
                    || !scheduleEvent.getStartTime().isAfter(now)) {
                expireRuleTasks(rule.getId());
                continue;
            }

            // 只有当用户已经不再是参与人时，才过期
            // 不再用 accepted 直接过期，避免 owner 或已确认用户导致提醒任务刚扫描就失效
            ScheduleParticipant participant = scheduleParticipantMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("scheduleId", rule.getScheduleId())
                            .eq("userId", rule.getUserId())
                            .eq("isDelete", 0)
            );
            if (participant == null) {
                expireRuleTasks(rule.getId());
                continue;
            }

            ScheduleReminderTask latestTask = findLatestTask(rule.getId());
            if (latestTask == null) {
                buildFirstTask(rule, scheduleEvent);
                latestTask = findLatestTask(rule.getId());
            }
            if (latestTask == null) {
                continue;
            }

            // 到点后 pending -> sent
            if (TASK_PENDING.equals(latestTask.getTaskStatus())
                    && !latestTask.getPlannedRemindTime().isAfter(now)) {
                ScheduleReminderTask update = new ScheduleReminderTask();
                update.setId(latestTask.getId());
                update.setActualRemindTime(now);
                update.setTaskStatus(TASK_SENT);
                update.setUpdateTime(now);
                scheduleReminderTaskMapper.update(update);

                latestTask = scheduleReminderTaskMapper.selectOneById(latestTask.getId());
            }

            // 如果上一条仍是 sent，说明前端还未确认；按规则继续创建下一次提醒
            if (TASK_SENT.equals(latestTask.getTaskStatus())) {
                Integer repeatCount = rule.getRepeatCount() == null ? 0 : rule.getRepeatCount();
                if (latestTask.getRemindIndex() >= repeatCount) {
                    continue;
                }

                LocalDateTime nextRemindTime = latestTask.getPlannedRemindTime()
                        .plusMinutes(rule.getRepeatIntervalMinutes());

                if (!nextRemindTime.isAfter(now) && nextRemindTime.isBefore(scheduleEvent.getStartTime())) {
                    ScheduleReminderTask nextTask = scheduleReminderTaskMapper.selectOneByQuery(
                            QueryWrapper.create()
                                    .eq("ruleId", rule.getId())
                                    .eq("remindIndex", latestTask.getRemindIndex() + 1)
                                    .eq("isDelete", 0)
                    );
                    if (nextTask == null) {
                        buildNextTask(rule, scheduleEvent, latestTask.getRemindIndex() + 1, nextRemindTime);
                    }
                }
            }
        }
    }

    private void rebuildFirstTask(ScheduleReminderRule rule, ScheduleEvent scheduleEvent) {
        scheduleReminderTaskMapper.deleteByQuery(
                QueryWrapper.create().eq("ruleId", rule.getId())
        );

        if (!STATUS_ENABLED.equals(rule.getStatus()) || !Objects.equals(rule.getPopupEnabled(), 1)) {
            return;
        }

        buildFirstTask(rule, scheduleEvent);
    }

    private void buildFirstTask(ScheduleReminderRule rule, ScheduleEvent scheduleEvent) {
        LocalDateTime plannedTime = scheduleEvent.getStartTime().minusMinutes(rule.getRemindOffsetMinutes());

        if (!plannedTime.isBefore(scheduleEvent.getStartTime())) {
            return;
        }
        if (plannedTime.isBefore(LocalDateTime.now().minusDays(1))) {
            return;
        }

        buildNextTask(rule, scheduleEvent, 0, plannedTime);
    }

    private void buildNextTask(ScheduleReminderRule rule, ScheduleEvent scheduleEvent, int remindIndex, LocalDateTime plannedTime) {
        ScheduleReminderTask task = new ScheduleReminderTask();
        task.setRuleId(rule.getId());
        task.setScheduleId(scheduleEvent.getId());
        task.setUserId(rule.getUserId());
        task.setRemindIndex(remindIndex);
        task.setPlannedRemindTime(plannedTime);
        task.setTaskStatus(TASK_PENDING);
        task.setPopupTitle("日程提醒：" + scheduleEvent.getTitle());
        task.setPopupContent("你的日程《" + scheduleEvent.getTitle() + "》将于 " + scheduleEvent.getStartTime()
                + " 开始，如未确认将按间隔继续提醒。");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setIsDelete(0);
        scheduleReminderTaskMapper.insert(task);
    }

    private ScheduleReminderTask findLatestTask(Long ruleId) {
        List<ScheduleReminderTask> list = scheduleReminderTaskMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("ruleId", ruleId)
                        .eq("isDelete", 0)
                        .orderBy("remindIndex", false)
        );
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    private void expireRuleTasks(Long ruleId) {
        List<ScheduleReminderTask> taskList = scheduleReminderTaskMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("ruleId", ruleId)
                        .eq("isDelete", 0)
        );

        for (ScheduleReminderTask task : taskList) {
            if (TASK_READ.equals(task.getTaskStatus()) || TASK_EXPIRED.equals(task.getTaskStatus())) {
                continue;
            }
            ScheduleReminderTask update = new ScheduleReminderTask();
            update.setId(task.getId());
            update.setTaskStatus(TASK_EXPIRED);
            update.setUpdateTime(LocalDateTime.now());
            scheduleReminderTaskMapper.update(update);
        }
    }

    private void checkScheduleAccess(ScheduleEvent scheduleEvent, User loginUser) {
        if (userService.isAdmin(loginUser) && Objects.equals(scheduleEvent.getCreatorId(), loginUser.getId())) {
            return;
        }

        ScheduleParticipant participant = scheduleParticipantMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("scheduleId", scheduleEvent.getId())
                        .eq("userId", loginUser.getId())
                        .eq("isDelete", 0)
        );
        if (participant == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权为该日程设置提醒");
        }
    }
}