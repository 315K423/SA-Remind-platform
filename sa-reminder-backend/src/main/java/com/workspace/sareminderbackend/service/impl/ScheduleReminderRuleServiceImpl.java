package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ScheduleReminderRuleServiceImpl
 * 实现日程提醒策略相关业务逻辑
 * 提供增删改查、任务生成、定时分发等功能
 */
@Service
public class ScheduleReminderRuleServiceImpl extends ServiceImpl<ScheduleReminderRuleMapper, ScheduleReminderRule>
        implements ScheduleReminderRuleService {

    // 常量定义：策略状态
    public static final String STATUS_ENABLED = "enabled"; // 启用
    public static final String STATUS_DISABLED = "disabled"; // 禁用

    // 常量定义：任务状态
    public static final String TASK_PENDING = "pending"; // 待提醒
    public static final String TASK_SENT = "sent";       // 已发送
    public static final String TASK_READ = "read";       // 已阅读
    public static final String TASK_EXPIRED = "expired"; // 已过期

    @Resource
    private ScheduleEventMapper scheduleEventMapper; // 日程数据访问

    @Resource
    private ScheduleParticipantMapper scheduleParticipantMapper; // 参与者数据访问

    @Resource
    private ScheduleReminderTaskMapper scheduleReminderTaskMapper; // 提醒任务数据访问

    @Resource
    private UserService userService; // 用户相关服务

    /**
     * 新增或更新提醒策略
     * @param request 保存请求
     * @param loginUser 当前登录用户
     * @return 新增或更新的策略ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveOrUpdateRule(ScheduleReminderRuleSaveRequest request, User loginUser) {
        // 参数校验
        if (request == null || request.getScheduleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Integer remindOffsetMinutes = request.getRemindOffsetMinutes(); // 提前提醒时间
        Integer repeatIntervalMinutes = request.getRepeatIntervalMinutes(); // 重复间隔时间

        // 提醒时间范围校验
        if (remindOffsetMinutes == null || remindOffsetMinutes < 0 || remindOffsetMinutes > 30 * 24 * 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提醒时间范围不合法");
        }

        // 重复间隔校验
        if (repeatIntervalMinutes == null || repeatIntervalMinutes < 1 || repeatIntervalMinutes > 24 * 60) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复间隔不合法");
        }

        // 获取日程
        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(request.getScheduleId());
        if (scheduleEvent == null || (scheduleEvent.getIsDelete() != null && scheduleEvent.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "日程不存在");
        }

        // 权限校验
        checkScheduleAccess(scheduleEvent, loginUser);

        // 判断日程是否已经开始或过期
        if (scheduleEvent.getStartTime() == null || !scheduleEvent.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已开始或已过期的日程不允许再设置提醒");
        }

        // 查询当前用户是否已经存在该日程的提醒策略
        ScheduleReminderRule exist = this.mapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("scheduleId", request.getScheduleId())
                        .eq("userId", loginUser.getId())
                        .eq("isDelete", 0)
        );

        LocalDateTime now = LocalDateTime.now(); // 当前时间

        // 新建提醒策略
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

            // 保存策略
            boolean saved = this.save(rule);
            if (!saved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存提醒策略失败");
            }

            // 重建首个提醒任务
            rebuildFirstTask(rule, scheduleEvent);
            return rule.getId();
        }

        // 更新已有策略
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

        // 更新策略后重建首个任务
        ScheduleReminderRule latest = this.getById(exist.getId());
        rebuildFirstTask(latest, scheduleEvent);
        return latest.getId();
    }

    /**
     * 删除提醒策略
     * @param id 策略ID
     * @param loginUser 当前登录用户
     * @return 删除是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRule(long id, User loginUser) {
        ScheduleReminderRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 非管理员且不是创建者，无权限删除
        if (!userService.isAdmin(loginUser) && !Objects.equals(rule.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除关联的提醒任务
        scheduleReminderTaskMapper.deleteByQuery(QueryWrapper.create().eq("ruleId", id));
        return this.removeById(id);
    }

    /**
     * 根据请求构建查询条件
     */
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

    /**
     * 获取VO对象
     */
    @Override
    public ScheduleReminderRuleVO getScheduleReminderRuleVO(ScheduleReminderRule rule) {
        if (rule == null) {
            return null;
        }
        ScheduleReminderRuleVO vo = new ScheduleReminderRuleVO();
        BeanUtil.copyProperties(rule, vo); // 复制属性
        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(rule.getScheduleId());
        if (scheduleEvent != null) {
            vo.setScheduleTitle(scheduleEvent.getTitle());
            vo.setScheduleStartTime(scheduleEvent.getStartTime());
        }
        return vo;
    }

    /**
     * 获取VO列表
     */
    @Override
    public List<ScheduleReminderRuleVO> getScheduleReminderRuleVOList(List<ScheduleReminderRule> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(this::getScheduleReminderRuleVO).collect(Collectors.toList());
    }

    /**
     * 扫描并触发一次提醒任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanAndDispatchOnce() {
        LocalDateTime now = LocalDateTime.now();

        // 获取所有启用且弹窗启用的策略
        List<ScheduleReminderRule> ruleList = this.list(
                QueryWrapper.create()
                        .eq("status", STATUS_ENABLED)
                        .eq("popupEnabled", 1)
                        .eq("isDelete", 0)
        );

        for (ScheduleReminderRule rule : ruleList) {
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(rule.getScheduleId());

            // 若日程不存在或已过期，过期该策略的所有任务
            if (scheduleEvent == null
                    || (scheduleEvent.getIsDelete() != null && scheduleEvent.getIsDelete() == 1)
                    || scheduleEvent.getStartTime() == null
                    || !scheduleEvent.getStartTime().isAfter(now)) {
                expireRuleTasks(rule.getId());
                continue;
            }

            // 检查参与者是否存在
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

            // 查找最新任务
            ScheduleReminderTask latestTask = findLatestTask(rule.getId());
            if (latestTask == null) {
                buildFirstTask(rule, scheduleEvent);
                latestTask = findLatestTask(rule.getId());
            }
            if (latestTask == null) {
                continue;
            }

            // 若任务为待提醒且到时间，则标记为已发送
            if (TASK_PENDING.equals(latestTask.getTaskStatus())
                    && latestTask.getPlannedRemindTime() != null
                    && !latestTask.getPlannedRemindTime().isAfter(now)) {
                ScheduleReminderTask update = new ScheduleReminderTask();
                update.setId(latestTask.getId());
                update.setActualRemindTime(now);
                update.setTaskStatus(TASK_SENT);
                update.setUpdateTime(now);
                scheduleReminderTaskMapper.update(update);

                latestTask = scheduleReminderTaskMapper.selectOneById(latestTask.getId());
            }

            // 若已发送且未达到重复次数，则生成下一次提醒
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

    /**
     * 重建首个任务
     */
    private void rebuildFirstTask(ScheduleReminderRule rule, ScheduleEvent scheduleEvent) {
        // 删除现有任务
        scheduleReminderTaskMapper.deleteByQuery(
                QueryWrapper.create().eq("ruleId", rule.getId())
        );

        if (!STATUS_ENABLED.equals(rule.getStatus()) || !Objects.equals(rule.getPopupEnabled(), 1)) {
            return;
        }

        buildFirstTask(rule, scheduleEvent);
    }

    /**
     * 构建首个任务
     */
    private void buildFirstTask(ScheduleReminderRule rule, ScheduleEvent scheduleEvent) {
        LocalDateTime plannedTime = scheduleEvent.getStartTime().minusMinutes(rule.getRemindOffsetMinutes());

        // 若计划时间非法则不创建
        if (!plannedTime.isBefore(scheduleEvent.getStartTime())) {
            return;
        }
        if (plannedTime.isBefore(LocalDateTime.now().minusDays(1))) {
            return;
        }

        buildNextTask(rule, scheduleEvent, 0, plannedTime);
    }

    /**
     * 构建下一次提醒任务（支持 upsert）
     */
    private void buildNextTask(ScheduleReminderRule rule, ScheduleEvent scheduleEvent, int remindIndex, LocalDateTime plannedTime) {
        LocalDateTime now = LocalDateTime.now();

        ScheduleReminderTask task = new ScheduleReminderTask();
        task.setId(IdUtil.getSnowflakeNextId());
        task.setRuleId(rule.getId());
        task.setScheduleId(scheduleEvent.getId());
        task.setUserId(rule.getUserId());
        task.setRemindIndex(remindIndex);
        task.setPlannedRemindTime(plannedTime);
        task.setActualRemindTime(null);
        task.setTaskStatus(TASK_PENDING);
        task.setPopupTitle("日程提醒：" + scheduleEvent.getTitle());
        task.setPopupContent("你的日程《" + scheduleEvent.getTitle() + "》将于 " + scheduleEvent.getStartTime()
                + " 开始，如未确认将按间隔继续提醒。");
        task.setReadTime(null);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setIsDelete(0);

        int affected = scheduleReminderTaskMapper.upsertTask(task);
        if (affected <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成提醒任务失败");
        }
    }

    /**
     * 查找规则下最新任务
     */
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

    /**
     * 将规则下所有未完成任务标记过期
     */
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

    /**
     * 校验用户是否有权限为该日程设置提醒
     */
    private void checkScheduleAccess(ScheduleEvent scheduleEvent, User loginUser) {
        // 管理员且创建者可直接操作
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