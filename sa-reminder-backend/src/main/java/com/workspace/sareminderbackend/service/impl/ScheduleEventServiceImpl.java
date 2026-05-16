package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.ScheduleDepartmentMapper;
import com.workspace.sareminderbackend.mapper.ScheduleEventMapper;
import com.workspace.sareminderbackend.mapper.ScheduleParticipantMapper;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventAddRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleDepartment;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleParticipant;
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.model.vo.ScheduleCalendarDayVO;
import com.workspace.sareminderbackend.model.vo.ScheduleConflictVO;
import com.workspace.sareminderbackend.model.vo.ScheduleEventSaveVO;
import com.workspace.sareminderbackend.model.vo.ScheduleEventVO;
import com.workspace.sareminderbackend.service.DepartmentService;
import com.workspace.sareminderbackend.service.ScheduleEventService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
    public class ScheduleEventServiceImpl extends ServiceImpl<ScheduleEventMapper, ScheduleEvent> implements ScheduleEventService {

    public static final String TYPE_PERSONAL = "personal";
    public static final String TYPE_MEETING = "meeting";
    public static final String TYPE_ATTENDANCE = "attendance";
    public static final String TYPE_COMPANY = "company";
    public static final String VIS_PRIVATE = "private";
    public static final String VIS_PUBLIC = "public";
    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_CANCELLED = "cancelled";

    @Resource
    private ScheduleParticipantMapper scheduleParticipantMapper;

    @Resource
    private ScheduleDepartmentMapper scheduleDepartmentMapper;

    @Resource
    private UserService userService;

    @Resource
    private DepartmentService departmentService;

    @Override
    public ScheduleEventSaveVO addScheduleEvent(ScheduleEventAddRequest request, User loginUser) {
        validateBasic(request.getTitle(), request.getStartTime(), request.getEndTime());
        validateCheckInConfig(request.getScheduleType(), request.getCheckInEnabled(), request.getCheckInLatitude(), request.getCheckInLongitude(), request.getCheckInRadiusMeters());
        RoleScope roleScope = buildRoleScopeForCreate(request.getDepartmentIdList(), request.getParticipantUserIdList(), loginUser);

        // 检查时间冲突
        List<ScheduleConflictVO> conflictList = findConflicts(null, request.getStartTime(), request.getEndTime(), loginUser, roleScope.participantIds);
        if (CollUtil.isNotEmpty(conflictList)) {
            return buildConflictResult(conflictList);
        }
        ScheduleEvent scheduleEvent = new ScheduleEvent();
        BeanUtil.copyProperties(request, scheduleEvent);
        String finalScheduleType = resolveScheduleType(request.getScheduleType(), loginUser);
        scheduleEvent.setScheduleType(finalScheduleType);
        scheduleEvent.setVisibility(StrUtil.blankToDefault(request.getVisibility(), VIS_PRIVATE));
        scheduleEvent.setStatus(STATUS_NORMAL);
        scheduleEvent.setAllDay(request.getAllDay() == null ? 0 : request.getAllDay());

        // 考勤启用逻辑
        int finalCheckInEnabled = request.getCheckInEnabled() == null ? (TYPE_ATTENDANCE.equals(finalScheduleType) ? 1 : 0) : request.getCheckInEnabled();
        scheduleEvent.setCheckInEnabled(finalCheckInEnabled);
        if (Objects.equals(finalCheckInEnabled, 1)) {
            scheduleEvent.setCheckInRadiusMeters(request.getCheckInRadiusMeters() == null ? 200 : request.getCheckInRadiusMeters());
        } else {
            scheduleEvent.setCheckInAddress(null);
            scheduleEvent.setCheckInLatitude(null);
            scheduleEvent.setCheckInLongitude(null);
            scheduleEvent.setCheckInRadiusMeters(null);
        }
        scheduleEvent.setCreatorId(loginUser.getId());
        LocalDateTime now = LocalDateTime.now();
        scheduleEvent.setEditTime(now);
        scheduleEvent.setCreateTime(now);
        scheduleEvent.setUpdateTime(now);
        scheduleEvent.setIsDelete(0);

        // 保存日程
        boolean saved = this.save(scheduleEvent);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建日程失败");
        }

        // 同步参与者与部门
        syncParticipants(scheduleEvent.getId(), roleScope.participantIds, loginUser.getId());
        syncDepartments(scheduleEvent.getId(), roleScope.departmentIds);

        // 返回结果
        ScheduleEventSaveVO result = new ScheduleEventSaveVO();
        result.setSuccess(true);
        result.setEventId(scheduleEvent.getId());
        result.setConflictDetected(false);
        return result;
    }

    @Override
    public ScheduleEventSaveVO updateScheduleEvent(ScheduleEventUpdateRequest request, User loginUser) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取原日程
        ScheduleEvent old = this.getById(request.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        if (!canEditSchedule(old, loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权修改该日程");
        }
        LocalDateTime startTime = request.getStartTime() == null ? old.getStartTime() : request.getStartTime();
        LocalDateTime endTime = request.getEndTime() == null ? old.getEndTime() : request.getEndTime();

        // 校验基本信息和签到配置
        validateBasic(StrUtil.blankToDefault(request.getTitle(), old.getTitle()), startTime, endTime);
        validateCheckInConfig(
                StrUtil.blankToDefault(request.getScheduleType(), old.getScheduleType()),
                request.getCheckInEnabled() == null ? old.getCheckInEnabled() : request.getCheckInEnabled(),
                request.getCheckInLatitude() == null ? old.getCheckInLatitude() : request.getCheckInLatitude(),
                request.getCheckInLongitude() == null ? old.getCheckInLongitude() : request.getCheckInLongitude(),
                request.getCheckInRadiusMeters() == null ? old.getCheckInRadiusMeters() : request.getCheckInRadiusMeters()
        );
        List<Long> participantIds = request.getParticipantUserIdList() == null ? getParticipantIds(old.getId()) : request.getParticipantUserIdList();
        List<Long> departmentIds = request.getDepartmentIdList() == null ? getDepartmentIds(old.getId()) : request.getDepartmentIdList();
        RoleScope roleScope = buildRoleScopeForCreate(departmentIds, participantIds, loginUser);

        // 保证原创建者参与
        if (!roleScope.participantIds.contains(old.getCreatorId())) {
            roleScope.participantIds.add(old.getCreatorId());
        }

        // 检查冲突
        List<ScheduleConflictVO> conflictList = findConflicts(old.getId(), startTime, endTime, loginUser, roleScope.participantIds);
        if (CollUtil.isNotEmpty(conflictList)) {
            return buildConflictResult(conflictList);
        }
        ScheduleEvent update = new ScheduleEvent();
        BeanUtil.copyProperties(request, update);
        update.setId(old.getId());
        String finalScheduleType = StrUtil.blankToDefault(request.getScheduleType(), old.getScheduleType());
        update.setScheduleType(finalScheduleType);
        int finalCheckInEnabled = request.getCheckInEnabled() == null
                ? (old.getCheckInEnabled() == null ? (TYPE_ATTENDANCE.equals(finalScheduleType) ? 1 : 0) : old.getCheckInEnabled())
                : request.getCheckInEnabled();
        update.setCheckInEnabled(finalCheckInEnabled);
        if (Objects.equals(finalCheckInEnabled, 1)) {
            update.setCheckInRadiusMeters(request.getCheckInRadiusMeters() == null
                    ? (old.getCheckInRadiusMeters() == null ? 200 : old.getCheckInRadiusMeters())
                    : request.getCheckInRadiusMeters());
        } else {
            update.setCheckInAddress(null);
            update.setCheckInLatitude(null);
            update.setCheckInLongitude(null);
            update.setCheckInRadiusMeters(null);
        }
        update.setUpdateTime(LocalDateTime.now());
        update.setEditTime(LocalDateTime.now());
        boolean ok = this.updateById(update);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新日程失败");
        }
        syncParticipants(old.getId(), roleScope.participantIds, old.getCreatorId());
        syncDepartments(old.getId(), roleScope.departmentIds);
        ScheduleEventSaveVO result = new ScheduleEventSaveVO();
        result.setSuccess(true);
        result.setEventId(old.getId());
        result.setConflictDetected(false);
        return result;
    }

    @Override
    public boolean deleteScheduleEvent(long id, User loginUser) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ScheduleEvent old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!canEditSchedule(old, loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        scheduleParticipantMapper.deleteByQuery(QueryWrapper.create().eq("scheduleId", id));
        scheduleDepartmentMapper.deleteByQuery(QueryWrapper.create().eq("scheduleId", id));
        return true;
    }

    /**
     *  构建日程查询条件封装
     *
     * @param request
     * @param loginUser
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ScheduleEventQueryRequest request, User loginUser) {

        // 构建基础查询条件
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("id", request.getId())
                .like("title", request.getTitle())
                .eq("scheduleType", request.getScheduleType());

        // 起止时间过滤
        if (request.getStartTimeFrom() != null) {
            wrapper.ge("startTime", request.getStartTimeFrom());
        }
        if (request.getStartTimeTo() != null) {
            wrapper.le("startTime", request.getStartTimeTo());
        }

        // 参与者子查询
        QueryWrapper participantSub = QueryWrapper.create().select("scheduleId").from("schedule_participant").eq("isDelete", 0);
        if (request.getParticipantUserId() != null) {
            participantSub.eq("userId", request.getParticipantUserId());
            wrapper.in("id", participantSub);
        }

        // 部门子查询
        if (request.getDepartmentId() != null) {
            QueryWrapper deptSub = QueryWrapper.create().select("scheduleId").from("schedule_department").eq("isDelete", 0)
                    .eq("departmentId", request.getDepartmentId());
            wrapper.in("id", deptSub);
        }

        // 权限控制
        if (userService.isAdmin(loginUser)) {
            wrapper.eq("creatorId", request.getCreatorId());
        } else if (userService.isManager(loginUser)) {
            Long deptId = loginUser.getDepartmentId();
            QueryWrapper joinSub = QueryWrapper.create().select("scheduleId").from("schedule_participant").eq("isDelete", 0)
                    .eq("userId", loginUser.getId());
            QueryWrapper deptSub = QueryWrapper.create().select("scheduleId").from("schedule_department").eq("isDelete", 0);
            if (deptId != null) {
                deptSub.eq("departmentId", deptId);
            } else {
                deptSub.eq("departmentId", -1L);
            }
            wrapper.and((Consumer<QueryWrapper>) q -> q
                    .eq("creatorId", loginUser.getId())
                    .or((Consumer<QueryWrapper>) o -> o.in("id", joinSub))
                    .or((Consumer<QueryWrapper>) o -> o.in("id", deptSub)));
        } else {
            QueryWrapper joinSub = QueryWrapper.create().select("scheduleId").from("schedule_participant").eq("isDelete", 0)
                    .eq("userId", loginUser.getId());
            wrapper.and((Consumer<QueryWrapper>) q -> q
                    .eq("creatorId", loginUser.getId())
                    .or((Consumer<QueryWrapper>) o -> o.in("id", joinSub)));
        }
        return wrapper.orderBy(request.getSortField(), "ascend".equals(request.getSortOrder()));
    }

    /**
     * 将ScheduleEvent对象转换为VO对象
     */
    @Override
    public ScheduleEventVO getScheduleEventVO(ScheduleEvent scheduleEvent) {
        if (scheduleEvent == null) {
            return null;
        }
        ScheduleEventVO vo = new ScheduleEventVO();
        BeanUtil.copyProperties(scheduleEvent, vo);
        vo.setParticipantUserIdList(getParticipantIds(scheduleEvent.getId()));
        vo.setDepartmentIdList(getDepartmentIds(scheduleEvent.getId()));
        return vo;
    }

    /**
     * 批量转换ScheduleEvent对象为VO列表
     */
    @Override
    public List<ScheduleEventVO> getScheduleEventVOList(List<ScheduleEvent> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream().map(this::getScheduleEventVO).collect(Collectors.toList());
    }

    /**
     * 获取用户指定月份的日程列表
     *
     * @param year 年
     * @param month 月
     * @param loginUser 当前登录用户
     */
    @Override
    public List<ScheduleEventVO> listMyMonthSchedule(int year, int month, User loginUser) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 获取可访问的日程
        List<ScheduleEvent> list = listAccessibleOverlapEvents(start, end, loginUser);

        // 按开始时间排序
        list.sort(Comparator.comparing(ScheduleEvent::getStartTime));
        return getScheduleEventVOList(list);
    }


    /**
     * 获取用户指定日期的日程列表（按日视图）
     */
    @Override
    public ScheduleCalendarDayVO getMyDaySchedule(LocalDate date, User loginUser) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<ScheduleEvent> list = listAccessibleOverlapEvents(start, end, loginUser);

        list.sort(Comparator.comparing(ScheduleEvent::getStartTime));

        ScheduleCalendarDayVO vo = new ScheduleCalendarDayVO();
        vo.setDate(date.toString());
        vo.setScheduleList(getScheduleEventVOList(list));
        return vo;
    }

    /**
     * 获取指定时间范围内用户可访问的日程列表
     */
    private List<ScheduleEvent> listAccessibleOverlapEvents(LocalDateTime start, LocalDateTime end, User loginUser) {
        ScheduleEventQueryRequest request = new ScheduleEventQueryRequest();
        QueryWrapper wrapper = getQueryWrapper(request, loginUser);
        wrapper.lt("startTime", end).gt("endTime", start);
        return this.list(wrapper);
    }

    /**
     * 构建参与者和部门权限范围
     */
    private RoleScope buildRoleScopeForCreate(List<Long> departmentIdList, List<Long> participantUserIdList, User loginUser) {
        RoleScope scope = new RoleScope();

        // 参与者ID列表去重
        List<Long> participantIds = Optional.ofNullable(participantUserIdList).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new));

        // 部门ID列表去重
        List<Long> departmentIds = Optional.ofNullable(departmentIdList).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new));

        // 默认加入创建者
        participantIds.add(loginUser.getId());
        if (userService.isAdmin(loginUser)) {
            // 管理员：验证参与者和部门有效性
            for (Long departmentId : departmentIds) {
                departmentService.getValidDepartment(departmentId);
            }
            for (Long userId : participantIds) {
                User user = userService.getById(userId);
                if (user == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "参与人不存在: " + userId);
                }
                if (!departmentIds.isEmpty() && user.getDepartmentId() != null && !departmentIds.contains(user.getDepartmentId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "参与人不在所选部门中: " + user.getUserName());
                }
                if (user.getDepartmentId() != null) {
                    departmentIds.add(user.getDepartmentId());
                }
            }
        } else if (userService.isManager(loginUser)) {
            // 部门经理：只能选择自己部门
            if (loginUser.getDepartmentId() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "部门经理尚未分配部门");
            }
            if (CollUtil.isNotEmpty(departmentIds) && departmentIds.stream().anyMatch(id -> !Objects.equals(id, loginUser.getDepartmentId()))) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "部门经理只能选择自己部门");
            }
            departmentIds.clear();
            departmentIds.add(loginUser.getDepartmentId());
            for (Long userId : participantIds) {
                User user = userService.getById(userId);
                if (user == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "参与人不存在: " + userId);
                }
                userService.validateDepartmentUser(user, loginUser.getDepartmentId());
            }
        } else {
            // 普通员工：只能为自己创建日程
            if (participantIds.stream().anyMatch(id -> !Objects.equals(id, loginUser.getId()))) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "普通员工只能为自己创建日程");
            }
            participantIds.clear();
            participantIds.add(loginUser.getId());
            departmentIds.clear();
            if (loginUser.getDepartmentId() != null) {
                departmentIds.add(loginUser.getDepartmentId());
            }
        }
        scope.participantIds = participantIds.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
        scope.departmentIds = departmentIds.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
        return scope;
    }

    /**
     * 解析最终日程类型并检查权限
     */
    private String resolveScheduleType(String scheduleType, User loginUser) {
        String finalType = StrUtil.blankToDefault(scheduleType, TYPE_PERSONAL);
        if (!TYPE_PERSONAL.equals(finalType)
                && !TYPE_MEETING.equals(finalType)
                && !TYPE_ATTENDANCE.equals(finalType)
                && !TYPE_COMPANY.equals(finalType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的日程类型");
        }
        if (TYPE_COMPANY.equals(finalType) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可创建 company 日程");
        }
        return finalType;
    }

    /**
     * 检查时间冲突
     */
    private List<ScheduleConflictVO> findConflicts(Long excludeScheduleId, LocalDateTime startTime, LocalDateTime endTime,
                                                   User loginUser, List<Long> participantIds) {
        Map<String, ScheduleConflictVO> conflictMap = new LinkedHashMap<>();

        // 检查创建者冲突
        addUserConflict(excludeScheduleId, startTime, endTime, loginUser.getId(), loginUser.getUserName(), "CREATOR", conflictMap);

        // 检查参与者冲突
        for (Long userId : participantIds) {
            if (Objects.equals(userId, loginUser.getId())) {
                continue;
            }
            User user = userService.getById(userId);
            if (user == null) {
                continue;
            }
            addUserConflict(excludeScheduleId, startTime, endTime, userId, user.getUserName(), "PARTICIPANT", conflictMap);
        }
        return new ArrayList<>(conflictMap.values());
    }


    /**
     * 添加单用户冲突到Map
     */
    private void addUserConflict(Long excludeScheduleId, LocalDateTime startTime, LocalDateTime endTime,
                                 Long userId, String userName, String conflictType,
                                 Map<String, ScheduleConflictVO> conflictMap) {
        QueryWrapper participantSub = QueryWrapper.create().select("scheduleId").from("schedule_participant")
                .eq("userId", userId).eq("isDelete", 0);
        QueryWrapper wrapper = QueryWrapper.create()
                .lt("startTime", endTime)
                .gt("endTime", startTime)
                .eq("status", STATUS_NORMAL)
                .eq("isDelete", 0)
                .and((Consumer<QueryWrapper>) q -> q.eq("creatorId", userId)
                        .or((Consumer<QueryWrapper>) o -> o.in("id", participantSub)));
        if (excludeScheduleId != null) {
            wrapper.ne("id", excludeScheduleId);
        }
        List<ScheduleEvent> conflictEvents = this.list(wrapper);
        for (ScheduleEvent event : conflictEvents) {
            String key = event.getId() + "_" + conflictType + "_" + userId;
            if (conflictMap.containsKey(key)) {
                continue;
            }
            ScheduleConflictVO vo = new ScheduleConflictVO();
            vo.setScheduleId(event.getId());
            vo.setTitle(event.getTitle());
            vo.setStartTime(event.getStartTime());
            vo.setEndTime(event.getEndTime());
            vo.setConflictType(conflictType);
            vo.setUserId(userId);
            vo.setUserName(userName);
            conflictMap.put(key, vo);
        }
    }


    /**
     * 构建冲突返回结果
     */
    private ScheduleEventSaveVO buildConflictResult(List<ScheduleConflictVO> conflictList) {
        ScheduleEventSaveVO result = new ScheduleEventSaveVO();
        result.setSuccess(false);
        result.setConflictDetected(true);
        result.setConflictList(conflictList);
        return result;
    }


    /**
     * 校验基础信息
     */
    private void validateBasic(String title, LocalDateTime startTime, LocalDateTime endTime) {
        if (StrUtil.isBlank(title) || title.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题为空或过长");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始/结束时间不合法");
        }
    }


    /**
     * 校验签到配置
     */
    private void validateCheckInConfig(String scheduleType, Integer checkInEnabled, Double latitude,
                                       Double longitude, Integer radiusMeters) {
        String finalType = StrUtil.blankToDefault(scheduleType, TYPE_PERSONAL);
        Integer finalEnabled = checkInEnabled == null ? (TYPE_ATTENDANCE.equals(finalType) ? 1 : 0) : checkInEnabled;
        if (!TYPE_ATTENDANCE.equals(finalType) && Objects.equals(finalEnabled, 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅考勤事项支持定位签到");
        }
        if (!Objects.equals(finalEnabled, 1)) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "启用定位签到时必须设置签到坐标");
        }
        if (radiusMeters != null && radiusMeters <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "签到半径必须大于0");
        }
    }


    /**
     * 判断是否有编辑权限
     */
    private boolean canEditSchedule(ScheduleEvent scheduleEvent, User loginUser) {
        return userService.isAdmin(loginUser) || Objects.equals(scheduleEvent.getCreatorId(), loginUser.getId());
    }

    /**
     * 同步参与者
     */
    private void syncParticipants(Long scheduleId, List<Long> userIds, Long ownerId) {
        if (scheduleId == null || scheduleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日程 id 非法");
        }
        Set<Long> targetUserIdSet = Optional.ofNullable(userIds)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ownerId != null) {
            targetUserIdSet.add(ownerId);
        }

        List<ScheduleParticipant> existingList = scheduleParticipantMapper.selectListByQuery(
                QueryWrapper.create().eq("scheduleId", scheduleId)
        );

        Map<Long, ScheduleParticipant> existingMap = existingList.stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(
                        ScheduleParticipant::getUserId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        LocalDateTime now = LocalDateTime.now();

        for (Long userId : targetUserIdSet) {
            ScheduleParticipant existing = existingMap.get(userId);
            String finalRole = Objects.equals(userId, ownerId) ? "owner" : "participant";
            if (existing != null) {
                ScheduleParticipant update = new ScheduleParticipant();
                update.setId(existing.getId());
                update.setParticipantRole(finalRole);
                update.setResponseStatus("pending");
                update.setAttendanceStatus(existing.getAttendanceStatus() == null ? "not_checked" : existing.getAttendanceStatus());
                update.setIsDelete(0);
                update.setUpdateTime(now);
                scheduleParticipantMapper.update(update);
            } else {
                ScheduleParticipant p = new ScheduleParticipant();
                p.setScheduleId(scheduleId);
                p.setUserId(userId);
                p.setParticipantRole(finalRole);
                p.setResponseStatus("pending");
                p.setAttendanceStatus("not_checked");
                p.setJoinTime(now);
                p.setCreateTime(now);
                p.setUpdateTime(now);
                p.setIsDelete(0);
                scheduleParticipantMapper.insert(p);
            }
        }

        for (ScheduleParticipant existing : existingList) {
            Long userId = existing.getUserId();
            if (userId == null || targetUserIdSet.contains(userId)) {
                continue;
            }
            ScheduleParticipant update = new ScheduleParticipant();
            update.setId(existing.getId());
            update.setIsDelete(1);
            update.setUpdateTime(now);
            scheduleParticipantMapper.update(update);
        }
    }

    /**
     * 同步部门
     */
    private void syncDepartments(Long scheduleId, List<Long> departmentIds) {
        if (scheduleId == null || scheduleId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日程 id 非法");
        }
        Set<Long> targetDepartmentIdSet = Optional.ofNullable(departmentIds)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ScheduleDepartment> existingList = scheduleDepartmentMapper.selectListByQuery(
                QueryWrapper.create().eq("scheduleId", scheduleId)
        );

        Map<Long, ScheduleDepartment> existingMap = existingList.stream()
                .filter(item -> item.getDepartmentId() != null)
                .collect(Collectors.toMap(
                        ScheduleDepartment::getDepartmentId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        LocalDateTime now = LocalDateTime.now();
        for (Long departmentId : targetDepartmentIdSet) {
            ScheduleDepartment existing = existingMap.get(departmentId);
            if (existing != null) {
                ScheduleDepartment update = new ScheduleDepartment();
                update.setId(existing.getId());
                update.setIsDelete(0);
                update.setUpdateTime(now);
                scheduleDepartmentMapper.update(update);
            } else {
                ScheduleDepartment relation = new ScheduleDepartment();
                relation.setScheduleId(scheduleId);
                relation.setDepartmentId(departmentId);
                relation.setCreateTime(now);
                relation.setUpdateTime(now);
                relation.setIsDelete(0);
                scheduleDepartmentMapper.insert(relation);
            }
        }

        for (ScheduleDepartment existing : existingList) {
            Long departmentId = existing.getDepartmentId();
            if (departmentId == null || targetDepartmentIdSet.contains(departmentId)) {
                continue;
            }
            ScheduleDepartment update = new ScheduleDepartment();
            update.setId(existing.getId());
            update.setIsDelete(1);
            update.setUpdateTime(now);
            scheduleDepartmentMapper.update(update);
        }
    }

    /**
     * 获取日程参与者ID列表
     */
    private List<Long> getParticipantIds(Long scheduleId) {
        List<ScheduleParticipant> participants = scheduleParticipantMapper.selectListByQuery(QueryWrapper.create().eq("scheduleId", scheduleId).eq("isDelete", 0));
        return participants.stream().map(ScheduleParticipant::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 获取日程关联部门ID列表
     */
    private List<Long> getDepartmentIds(Long scheduleId) {
        List<ScheduleDepartment> departments = scheduleDepartmentMapper.selectListByQuery(QueryWrapper.create().eq("scheduleId", scheduleId).eq("isDelete", 0));
        return departments.stream().map(ScheduleDepartment::getDepartmentId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 内部类：角色作用域
     * 用于记录当前日程操作涉及的参与者ID和部门ID
     */
    private static class RoleScope {
        private List<Long> participantIds = new ArrayList<>();
        private List<Long> departmentIds = new ArrayList<>();
    }
}
