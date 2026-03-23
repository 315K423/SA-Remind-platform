package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
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
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderRuleVO;
import com.workspace.sareminderbackend.service.ScheduleReminderRuleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 日程提醒策略 服务实现。
 */
@Service
public class ScheduleReminderRuleServiceImpl extends ServiceImpl<ScheduleReminderRuleMapper, ScheduleReminderRule>
        implements ScheduleReminderRuleService {

    public static final String STATUS_ENABLED = "enabled";
    public static final String STATUS_DISABLED = "disabled";

    public static final String SCHEDULE_STATUS_CANCELLED = "cancelled";

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

    @Override
    public long saveOrUpdateRule(ScheduleReminderRuleSaveRequest request, User loginUser) {
        if (request == null || request.getScheduleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer remindOffsetMinutes = request.getRemindOffsetMinutes();
        Integer repeatCount = request.getRepeatCount();
        Integer repeatIntervalMinutes = request.getRepeatIntervalMinutes();
        if (remindOffsetMinutes == null || remindOffsetMinutes < 0 || remindOffsetMinutes > 30 * 24 * 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提醒时间范围不合法");
        }
        if (repeatCount == null || repeatCount < 0 || repeatCount > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复次数不合法");
        }
        if (repeatIntervalMinutes == null || repeatIntervalMinutes < 1 || repeatIntervalMinutes > 24 * 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复间隔不合法");
        }

        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(request.getScheduleId());
        if (scheduleEvent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "日程不存在");
        }
        checkScheduleAccess(scheduleEvent, loginUser);
        if (scheduleEvent.getStartTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日程开始时间不能为空");
        }
        if (scheduleEvent.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已开始或已过期的日程不允许再设置提醒");
        }

        ScheduleReminderRule exist = this.mapper.selectOneByQuery(QueryWrapper.create()
                .eq("scheduleId", request.getScheduleId())
                .eq("userId", loginUser.getId()));

        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            ScheduleReminderRule rule = new ScheduleReminderRule();
            rule.setScheduleId(request.getScheduleId());
            rule.setUserId(loginUser.getId());
            rule.setRemindOffsetMinutes(remindOffsetMinutes);
            rule.setRepeatCount(repeatCount);
            rule.setRepeatIntervalMinutes(repeatIntervalMinutes);
            rule.setPopupEnabled(request.getPopupEnabled() == null ? 1 : request.getPopupEnabled());
            rule.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
            rule.setLastGenerateTime(null);
            rule.setCreateTime(now);
            rule.setUpdateTime(now);
            rule.setIsDelete(0);
            boolean saved = this.save(rule);
            if (!saved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存提醒策略失败");
            }
            rebuildTasks(rule, scheduleEvent);
            return rule.getId();
        }

        ScheduleReminderRule update = new ScheduleReminderRule();
        update.setId(exist.getId());
        update.setRemindOffsetMinutes(remindOffsetMinutes);
        update.setRepeatCount(repeatCount);
        update.setRepeatIntervalMinutes(repeatIntervalMinutes);
        update.setPopupEnabled(request.getPopupEnabled() == null ? exist.getPopupEnabled() : request.getPopupEnabled());
        update.setStatus(request.getStatus() == null ? exist.getStatus() : request.getStatus());
        update.setUpdateTime(now);
        boolean ok = this.updateById(update);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提醒策略失败");
        }
        ScheduleReminderRule latest = this.getById(exist.getId());
        rebuildTasks(latest, scheduleEvent);
        return latest.getId();
    }

    @Override
    public boolean deleteRule(long id, User loginUser) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ScheduleReminderRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!isAdmin(loginUser) && !Objects.equals(rule.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        scheduleReminderTaskMapper.deleteByQuery(QueryWrapper.create().eq("ruleId", id));
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(ScheduleReminderRuleQueryRequest request, User loginUser) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("id", request.getId())
                .eq("scheduleId", request.getScheduleId())
                .eq("status", request.getStatus());

        if (isAdmin(loginUser)) {
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
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<ScheduleReminderRule> ruleList = this.list(QueryWrapper.create().eq("status", STATUS_ENABLED));
        for (ScheduleReminderRule rule : ruleList) {
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(rule.getScheduleId());
            if (scheduleEvent == null || SCHEDULE_STATUS_CANCELLED.equals(scheduleEvent.getStatus())) {
                continue;
            }
            generatePendingTasks(rule, scheduleEvent);
        }

        List<ScheduleReminderTask> pendingTaskList = scheduleReminderTaskMapper.selectListByQuery(QueryWrapper.create()
                .eq("taskStatus", TASK_PENDING)
                .le("plannedRemindTime", now));
        for (ScheduleReminderTask task : pendingTaskList) {
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(task.getScheduleId());
            if (scheduleEvent == null || scheduleEvent.getStartTime() == null) {
                continue;
            }
            if (scheduleEvent.getStartTime().isBefore(now.minusDays(1))) {
                ScheduleReminderTask expire = new ScheduleReminderTask();
                expire.setId(task.getId());
                expire.setTaskStatus(TASK_EXPIRED);
                scheduleReminderTaskMapper.update(expire);
                continue;
            }
            ScheduleReminderTask update = new ScheduleReminderTask();
            update.setId(task.getId());
            update.setActualRemindTime(LocalDateTime.now());
            update.setTaskStatus(TASK_SENT);
            scheduleReminderTaskMapper.update(update);
        }
    }

    private void rebuildTasks(ScheduleReminderRule rule, ScheduleEvent scheduleEvent) {
        scheduleReminderTaskMapper.deleteByQuery(QueryWrapper.create().eq("ruleId", rule.getId()));
        if (!STATUS_ENABLED.equals(rule.getStatus())) {
            return;
        }
        generatePendingTasks(rule, scheduleEvent);
    }

    private void generatePendingTasks(ScheduleReminderRule rule, ScheduleEvent scheduleEvent) {
        if (rule == null || scheduleEvent == null || scheduleEvent.getStartTime() == null) {
            return;
        }
        LocalDateTime startTime = scheduleEvent.getStartTime();
        LocalDateTime firstTime = startTime.minusMinutes(rule.getRemindOffsetMinutes());
        int maxIndex = rule.getRepeatCount() == null ? 0 : rule.getRepeatCount();
        int interval = rule.getRepeatIntervalMinutes() == null ? 5 : rule.getRepeatIntervalMinutes();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i <= maxIndex; i++) {
            LocalDateTime planned = firstTime.plusMinutes((long) i * interval);
            if (!planned.isBefore(startTime)) {
                continue;
            }
            ScheduleReminderTask exist = scheduleReminderTaskMapper.selectOneByQuery(QueryWrapper.create()
                    .eq("ruleId", rule.getId())
                    .eq("remindIndex", i));
            if (exist != null) {
                continue;
            }
            ScheduleReminderTask task = new ScheduleReminderTask();
            task.setRuleId(rule.getId());
            task.setScheduleId(scheduleEvent.getId());
            task.setUserId(rule.getUserId());
            task.setRemindIndex(i);
            task.setPlannedRemindTime(planned);
            task.setTaskStatus(planned.isBefore(now.minusDays(1)) ? TASK_EXPIRED : TASK_PENDING);
            task.setPopupTitle(buildPopupTitle(scheduleEvent, planned, startTime));
            task.setPopupContent(buildPopupContent(scheduleEvent, planned, startTime, i));
            task.setCreateTime(now);
            task.setUpdateTime(now);
            task.setIsDelete(0);
            scheduleReminderTaskMapper.insert(task);
        }

        ScheduleReminderRule update = new ScheduleReminderRule();
        update.setId(rule.getId());
        update.setLastGenerateTime(now);
        this.updateById(update);
    }

    private String buildPopupTitle(ScheduleEvent scheduleEvent, LocalDateTime planned, LocalDateTime startTime) {
        long minutes = ChronoUnit.MINUTES.between(planned, startTime);
        return "日程临期提醒：" + scheduleEvent.getTitle() + "（还有 " + minutes + " 分钟开始）";
    }

    private String buildPopupContent(ScheduleEvent scheduleEvent, LocalDateTime planned, LocalDateTime startTime, int index) {
        long minutes = ChronoUnit.MINUTES.between(planned, startTime);
        StringBuilder sb = new StringBuilder();
        sb.append("你的日程《").append(scheduleEvent.getTitle()).append("》即将开始");
        sb.append("，预计开始时间：").append(startTime);
        if (scheduleEvent.getLocation() != null) {
            sb.append("，地点：").append(scheduleEvent.getLocation());
        }
        sb.append("。当前为第 ").append(index + 1).append(" 次提醒");
        sb.append("，距离开始还有 ").append(minutes).append(" 分钟。");
        return sb.toString();
    }

    private void checkScheduleAccess(ScheduleEvent scheduleEvent, User loginUser) {
        if (isAdmin(loginUser)) {
            return;
        }
        if (Objects.equals(scheduleEvent.getCreatorId(), loginUser.getId())) {
            return;
        }
        ScheduleParticipant participant = scheduleParticipantMapper.selectOneByQuery(QueryWrapper.create()
                .eq("scheduleId", scheduleEvent.getId())
                .eq("userId", loginUser.getId()));
        if (participant == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权为该日程设置提醒");
        }
    }

    private boolean isAdmin(User loginUser) {
        return UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
    }
}
