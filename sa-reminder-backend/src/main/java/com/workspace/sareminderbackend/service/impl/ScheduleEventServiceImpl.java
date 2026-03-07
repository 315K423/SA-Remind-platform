package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.ScheduleEventMapper;
import com.workspace.sareminderbackend.mapper.ScheduleParticipantMapper;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventAddRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventUpdateRequest;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleParticipant;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.model.vo.ScheduleEventVO;
import com.workspace.sareminderbackend.service.ScheduleEventService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 日程事件 服务实现。
 */
@Service
public class ScheduleEventServiceImpl extends ServiceImpl<ScheduleEventMapper, ScheduleEvent>
        implements ScheduleEventService {

    public static final String TYPE_PERSONAL = "personal";
    public static final String TYPE_COMPANY = "company";

    public static final String VIS_PRIVATE = "private";
    public static final String VIS_PUBLIC = "public";

    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_CANCELLED = "cancelled";

    @Resource
    private ScheduleParticipantMapper scheduleParticipantMapper;

    @Override
    public long addScheduleEvent(ScheduleEventAddRequest request, User loginUser) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 基础校验
        String title = request.getTitle();
        if (StrUtil.isBlank(title) || title.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题为空或过长");
        }
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始/结束时间不合法");
        }
        String scheduleType = StrUtil.blankToDefault(request.getScheduleType(), TYPE_PERSONAL);
        String visibility = StrUtil.blankToDefault(request.getVisibility(), VIS_PRIVATE);

        // 2. 权限：company 仅管理员
        if (TYPE_COMPANY.equals(scheduleType)) {
            UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            if (!UserRoleEnum.ADMIN.equals(roleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可创建公司日程");
            }
        }

        // 3. 保存日程
        ScheduleEvent scheduleEvent = new ScheduleEvent();
        BeanUtil.copyProperties(request, scheduleEvent);
        scheduleEvent.setScheduleType(scheduleType);
        scheduleEvent.setVisibility(visibility);
        scheduleEvent.setStatus(STATUS_NORMAL);
        scheduleEvent.setAllDay(request.getAllDay() == null ? 0 : request.getAllDay());
        scheduleEvent.setCreatorId(loginUser.getId());
        scheduleEvent.setEditTime(LocalDateTime.now());
        scheduleEvent.setCreateTime(LocalDateTime.now());
        scheduleEvent.setUpdateTime(LocalDateTime.now());
        scheduleEvent.setIsDelete(0);

        boolean saved = this.save(scheduleEvent);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建日程失败");
        }

        // 4. 参与人处理：
        // - 默认把创建人写入 owner
        // - 普通员工只允许自己
        List<Long> participantIds = request.getParticipantUserIdList();
        List<Long> finalIds = new ArrayList<>();
        finalIds.add(loginUser.getId());
        if (CollUtil.isNotEmpty(participantIds)) {
            UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            if (UserRoleEnum.ADMIN.equals(roleEnum)) {
                finalIds.addAll(participantIds);
            } else {
                // 普通用户：若传入包含非本人，直接拒绝
                boolean hasOther = participantIds.stream().anyMatch(id -> !Objects.equals(id, loginUser.getId()));
                if (hasOther) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "普通用户只能为自己创建日程");
                }
            }
        }
        // 去重
        finalIds = finalIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        batchInsertParticipants(scheduleEvent.getId(), finalIds, loginUser.getId());

        return scheduleEvent.getId();
    }

    @Override
    public boolean updateScheduleEvent(ScheduleEventUpdateRequest request, User loginUser) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = request.getId();
        ScheduleEvent old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 权限：管理员 或 创建人
        if (!isAdmin(loginUser) && !Objects.equals(old.getCreatorId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 校验时间
        LocalDateTime startTime = request.getStartTime() == null ? old.getStartTime() : request.getStartTime();
        LocalDateTime endTime = request.getEndTime() == null ? old.getEndTime() : request.getEndTime();
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始/结束时间不合法");
        }

        ScheduleEvent toUpdate = new ScheduleEvent();
        BeanUtil.copyProperties(request, toUpdate);
        toUpdate.setEditTime(LocalDateTime.now());
        boolean updated = this.updateById(toUpdate);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新日程失败");
        }

        // 覆盖式更新参与人
        if (request.getParticipantUserIdList() != null) {
            // 先删旧（逻辑删除会在 isDelete 上打标；BaseMapper#deleteByQuery 走物理删除，这里用 update 方式）
            QueryWrapper delWrapper = QueryWrapper.create()
                    .eq("scheduleId", id);
            // 物理删除更简单（参与人本身可重建），并不影响主表逻辑删除语义
            scheduleParticipantMapper.deleteByQuery(delWrapper);

            List<Long> ids = request.getParticipantUserIdList().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            // 始终保留创建人
            if (!ids.contains(old.getCreatorId())) {
                ids.add(old.getCreatorId());
            }
            batchInsertParticipants(id, ids, old.getCreatorId());
        }

        return true;
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
        if (!isAdmin(loginUser) && !Objects.equals(old.getCreatorId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 参与人表物理删除（简化）
        scheduleParticipantMapper.deleteByQuery(QueryWrapper.create().eq("scheduleId", id));
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(ScheduleEventQueryRequest request, User loginUser) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = request.getId();
        String title = request.getTitle();
        String scheduleType = request.getScheduleType();
        LocalDateTime from = request.getStartTimeFrom();
        LocalDateTime to = request.getStartTimeTo();
        Long creatorId = request.getCreatorId();

        QueryWrapper wrapper = QueryWrapper.create()
                .eq("id", id)
                .like("title", title)
                .eq("scheduleType", scheduleType)
                .ge("startTime", from)
                .le("startTime", to);

        // 权限收敛：
        // - 管理员：可按 creatorId 查询，不传则查询全部
        // - 普通用户：只能看到自己创建的 或 自己参与的
        if (isAdmin(loginUser)) {
            wrapper.eq("creatorId", creatorId);
        } else {
            // 自己创建的
            // 或 参与的（子查询）
            QueryWrapper joinSub = QueryWrapper.create()
                    .select("scheduleId")
                    .from("schedule_participant")
                    .eq("userId", loginUser.getId());

            wrapper.and((Consumer<QueryWrapper>) q -> q
                    .eq("creatorId", loginUser.getId())
                    .or((Consumer<QueryWrapper>) o -> o.in("id", joinSub))
            );
        }

        return wrapper.orderBy(request.getSortField(), "ascend".equals(request.getSortOrder()));
    }

    @Override
    public ScheduleEventVO getScheduleEventVO(ScheduleEvent scheduleEvent) {
        if (scheduleEvent == null) {
            return null;
        }
        ScheduleEventVO vo = new ScheduleEventVO();
        BeanUtil.copyProperties(scheduleEvent, vo);
        // 参与人
        List<ScheduleParticipant> participants = scheduleParticipantMapper.selectListByQuery(
                QueryWrapper.create().eq("scheduleId", scheduleEvent.getId())
        );
        if (CollUtil.isNotEmpty(participants)) {
            vo.setParticipantUserIdList(participants.stream()
                    .map(ScheduleParticipant::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()));
        } else {
            vo.setParticipantUserIdList(new ArrayList<>());
        }
        return vo;
    }

    @Override
    public List<ScheduleEventVO> getScheduleEventVOList(List<ScheduleEvent> list) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 简化：逐条查参与人（数据量大时建议批量聚合）
        return list.stream().map(this::getScheduleEventVO).collect(Collectors.toList());
    }

    private boolean isAdmin(User loginUser) {
        UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        return UserRoleEnum.ADMIN.equals(roleEnum);
    }

    private void batchInsertParticipants(Long scheduleId, List<Long> userIds, Long ownerId) {
        if (scheduleId == null || CollUtil.isEmpty(userIds)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : userIds) {
            ScheduleParticipant p = new ScheduleParticipant();
            p.setScheduleId(scheduleId);
            p.setUserId(userId);
            p.setParticipantRole(Objects.equals(userId, ownerId) ? "owner" : "participant");
            p.setResponseStatus(Objects.equals(userId, ownerId) ? "accepted" : "pending");
            p.setJoinTime(now);
            p.setCreateTime(now);
            p.setUpdateTime(now);
            p.setIsDelete(0);
            scheduleParticipantMapper.insert(p);
        }
    }
}
