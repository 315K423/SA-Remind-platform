package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.ScheduleEventMapper;
import com.workspace.sareminderbackend.mapper.ScheduleParticipantMapper;
import com.workspace.sareminderbackend.mapper.ScheduleReminderTaskMapper;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceCheckInRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleAttendanceUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleParticipant;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceCheckInVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceRateVO;
import com.workspace.sareminderbackend.model.vo.ScheduleAttendanceVO;
import com.workspace.sareminderbackend.service.ScheduleAttendanceService;
import com.workspace.sareminderbackend.service.ScheduleReminderTaskService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleAttendanceServiceImpl implements ScheduleAttendanceService {

    public static final String ATTENDANCE_NOT_CHECKED = "not_checked";
    public static final String ATTENDANCE_CHECKED_IN = "checked_in";
    public static final String TASK_SENT = "sent";

    @Resource
    private ScheduleReminderTaskMapper scheduleReminderTaskMapper;

    @Resource
    private ScheduleParticipantMapper scheduleParticipantMapper;

    @Resource
    private ScheduleEventMapper scheduleEventMapper;

    @Resource
    private ScheduleReminderTaskService scheduleReminderTaskService;

    @Resource
    private UserService userService;

    /**
     *  根据任务进行签到
     *
     * @param request
     * @param loginUser
     * @return
     */
    @Override
    public ScheduleAttendanceCheckInVO checkInByTask(ScheduleAttendanceCheckInRequest request, User loginUser) {
        if (request == null || request.getTaskId() == null || request.getTaskId() <= 0
                || request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "签到参数不完整");
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取提醒任务
        ScheduleReminderTask task = scheduleReminderTaskMapper.selectOneById(request.getTaskId());
        if (task == null || (task.getIsDelete() != null && task.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提醒任务不存在");
        }
        if (!Objects.equals(task.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (!TASK_SENT.equals(task.getTaskStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前提醒未处于待签到状态");
        }

        // 获取日程
        ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(task.getScheduleId());
        if (scheduleEvent == null || (scheduleEvent.getIsDelete() != null && scheduleEvent.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "日程不存在");
        }
        if (!isAttendanceCheckRequired(scheduleEvent)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该日程未开启定位签到");
        }

        // 获取参与者
        ScheduleParticipant participant = scheduleParticipantMapper.selectOneByQuery(QueryWrapper.create()
                .eq("scheduleId", scheduleEvent.getId())
                .eq("userId", loginUser.getId())
                .eq("isDelete", 0));
        if (participant == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "你不是该日程参与人");
        }

        // 计算距离
        double distanceMeters = calculateDistanceMeters(
                request.getLatitude(),
                request.getLongitude(),
                scheduleEvent.getCheckInLatitude(),
                scheduleEvent.getCheckInLongitude()
        );
        int radiusMeters = scheduleEvent.getCheckInRadiusMeters() == null ? 200 : scheduleEvent.getCheckInRadiusMeters();

        ScheduleAttendanceCheckInVO result = new ScheduleAttendanceCheckInVO();
        result.setDistanceMeters(round(distanceMeters));
        result.setWithinRange(distanceMeters <= radiusMeters);

        // 判断超出签到范围
        if (distanceMeters > radiusMeters) {
            result.setSuccess(false);
            result.setAttendanceStatus(
                    participant.getAttendanceStatus() == null ? ATTENDANCE_NOT_CHECKED : participant.getAttendanceStatus()
            );
            result.setMessage("定位签到失败，距离签到点超过" + radiusMeters + "米");
            return result;
        }

        // 跟新签到记录
        LocalDateTime now = LocalDateTime.now();
        ScheduleParticipant update = new ScheduleParticipant();
        update.setId(participant.getId());
        update.setAttendanceStatus(ATTENDANCE_CHECKED_IN);
        update.setCheckInTime(now);
        update.setCheckInLatitude(request.getLatitude());
        update.setCheckInLongitude(request.getLongitude());
        update.setCheckInDistanceMeters(round(distanceMeters));
        update.setUpdateTime(now);
        scheduleParticipantMapper.update(update);

        // 定位成功后自动完成“确认收到”逻辑
        scheduleReminderTaskService.readPopupTask(task.getId(), loginUser);

        result.setSuccess(true);
        result.setAttendanceStatus(ATTENDANCE_CHECKED_IN);
        result.setMessage("签到成功");
        return result;
    }

    /**
     *  分页查询考勤记录
     *
     * @param request
     * @return
     */
    @Override
    public Page<ScheduleAttendanceVO> listAttendancePage(ScheduleAttendanceQueryRequest request) {
        final ScheduleAttendanceQueryRequest finalRequest =
                (request == null ? new ScheduleAttendanceQueryRequest() : request);

        // 查询所有参与者记录
        List<ScheduleParticipant> participantList = scheduleParticipantMapper.selectListByQuery(
                QueryWrapper.create().eq("isDelete", 0)
        );
        if (CollUtil.isEmpty(participantList)) {
            long pageNum = finalRequest.getPageNum() <= 0 ? 1 : finalRequest.getPageNum();
            long pageSize = finalRequest.getPageSize() <= 0 ? 10 : finalRequest.getPageSize();
            return new Page<>(pageNum, pageSize, 0);
        }

        // 构造AttendanceVO列表
        List<ScheduleAttendanceVO> allList = new ArrayList<>();
        for (ScheduleParticipant participant : participantList) {
            ScheduleEvent scheduleEvent = scheduleEventMapper.selectOneById(participant.getScheduleId());
            if (scheduleEvent == null || (scheduleEvent.getIsDelete() != null && scheduleEvent.getIsDelete() == 1)) {
                continue;
            }
            User user = userService.getById(participant.getUserId());
            if (user == null || (user.getIsDelete() != null && user.getIsDelete() == 1)) {
                continue;
            }

            ScheduleAttendanceVO vo = new ScheduleAttendanceVO();
            vo.setParticipantId(participant.getId());
            vo.setScheduleId(scheduleEvent.getId());
            vo.setScheduleTitle(scheduleEvent.getTitle());
            vo.setScheduleStartTime(scheduleEvent.getStartTime());
            vo.setScheduleEndTime(scheduleEvent.getEndTime());
            vo.setUserId(user.getId());
            vo.setUserName(user.getUserName());
            vo.setParticipantRole(participant.getParticipantRole());
            vo.setResponseStatus(participant.getResponseStatus());
            vo.setAttendanceStatus(
                    participant.getAttendanceStatus() == null ? ATTENDANCE_NOT_CHECKED : participant.getAttendanceStatus()
            );
            vo.setCheckInTime(participant.getCheckInTime());
            vo.setCheckInAddress(scheduleEvent.getCheckInAddress());
            vo.setCheckInRadiusMeters(scheduleEvent.getCheckInRadiusMeters());
            vo.setCheckInDistanceMeters(participant.getCheckInDistanceMeters());
            allList.add(vo);
        }

        // 根据条件过滤
        List<ScheduleAttendanceVO> filtered = allList.stream()
                .filter(item -> finalRequest.getScheduleId() == null || Objects.equals(item.getScheduleId(), finalRequest.getScheduleId()))
                .filter(item -> finalRequest.getUserId() == null || Objects.equals(item.getUserId(), finalRequest.getUserId()))
                .filter(item -> StrUtil.isBlank(finalRequest.getScheduleTitle()) || StrUtil.containsIgnoreCase(item.getScheduleTitle(), finalRequest.getScheduleTitle()))
                .filter(item -> StrUtil.isBlank(finalRequest.getUserName()) || StrUtil.containsIgnoreCase(item.getUserName(), finalRequest.getUserName()))
                .filter(item -> StrUtil.isBlank(finalRequest.getAttendanceStatus()) || StrUtil.equalsIgnoreCase(item.getAttendanceStatus(), finalRequest.getAttendanceStatus()))
                .sorted(Comparator.comparing(
                        ScheduleAttendanceVO::getScheduleStartTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                ).reversed())
                .collect(Collectors.toList());

        long total = filtered.size();
        int pageNum = finalRequest.getPageNum() <= 0 ? 1 : finalRequest.getPageNum();
        int pageSize = finalRequest.getPageSize() <= 0 ? 10 : finalRequest.getPageSize();
        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<ScheduleAttendanceVO> pageRecords =
                fromIndex >= filtered.size() ? new ArrayList<>() : filtered.subList(fromIndex, toIndex);

        Page<ScheduleAttendanceVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pageRecords);
        return page;
    }

    /**
     *  管理员修改考勤状态
     *
     * @param request
     * @return
     */
    @Override
    public boolean adminUpdateAttendanceStatus(ScheduleAttendanceUpdateRequest request) {
        if (request == null || request.getParticipantId() == null || request.getParticipantId() <= 0
                || StrUtil.isBlank(request.getAttendanceStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!ATTENDANCE_NOT_CHECKED.equals(request.getAttendanceStatus())
                && !ATTENDANCE_CHECKED_IN.equals(request.getAttendanceStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的考勤状态");
        }

        ScheduleParticipant participant = scheduleParticipantMapper.selectOneById(request.getParticipantId());
        if (participant == null || (participant.getIsDelete() != null && participant.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "参与记录不存在");
        }

        ScheduleParticipant update = new ScheduleParticipant();
        update.setId(participant.getId());
        update.setAttendanceStatus(request.getAttendanceStatus());
        update.setCheckInTime(ATTENDANCE_CHECKED_IN.equals(request.getAttendanceStatus()) ? LocalDateTime.now() : null);

        if (ATTENDANCE_NOT_CHECKED.equals(request.getAttendanceStatus())) {
            update.setCheckInLatitude(null);
            update.setCheckInLongitude(null);
            update.setCheckInDistanceMeters(null);
        }

        update.setUpdateTime(LocalDateTime.now());
        return scheduleParticipantMapper.update(update) > 0;
    }

    /**
     *  分页查询考勤率
     *
     * @param request
     * @return
     */
    @Override
    public Page<ScheduleAttendanceRateVO> listAttendanceRatePage(ScheduleAttendanceQueryRequest request) {
        ScheduleAttendanceQueryRequest finalRequest =
                request == null ? new ScheduleAttendanceQueryRequest() : request;

        // 查询所有考勤事件
        List<ScheduleEvent> eventList = scheduleEventMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("scheduleType", "attendance")
                        .eq("isDelete", 0)
        );

        // 构建考勤率VO表
        List<ScheduleAttendanceRateVO> allList = Optional.ofNullable(eventList)
                .orElse(new ArrayList<>())
                .stream()
                .filter(item -> finalRequest.getScheduleId() == null || Objects.equals(item.getId(), finalRequest.getScheduleId()))
                .filter(item -> StrUtil.isBlank(finalRequest.getScheduleTitle()) || StrUtil.containsIgnoreCase(item.getTitle(), finalRequest.getScheduleTitle()))
                .sorted(Comparator.comparing(
                        ScheduleEvent::getStartTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                ).reversed())
                .map(this::buildAttendanceRateVO)
                .collect(Collectors.toList());

        long total = allList.size();
        int pageNum = finalRequest.getPageNum() <= 0 ? 1 : finalRequest.getPageNum();
        int pageSize = finalRequest.getPageSize() <= 0 ? 10 : finalRequest.getPageSize();
        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, allList.size());
        List<ScheduleAttendanceRateVO> pageRecords =
                fromIndex >= allList.size() ? new ArrayList<>() : allList.subList(fromIndex, toIndex);

        Page<ScheduleAttendanceRateVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pageRecords);
        return page;
    }

    /**
     *  构建考勤率VO
     *
     * @param scheduleEvent
     * @return
     */
    private ScheduleAttendanceRateVO buildAttendanceRateVO(ScheduleEvent scheduleEvent) {
        List<ScheduleParticipant> participantList = scheduleParticipantMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("scheduleId", scheduleEvent.getId())
                        .eq("isDelete", 0)
        );

        int participantCount = CollUtil.isEmpty(participantList) ? 0 : participantList.size();
        int checkedCount = CollUtil.isEmpty(participantList) ? 0 : (int) participantList.stream()
                .filter(item -> ATTENDANCE_CHECKED_IN.equalsIgnoreCase(item.getAttendanceStatus()))
                .count();
        int uncheckedCount = Math.max(participantCount - checkedCount, 0);

        ScheduleAttendanceRateVO vo = new ScheduleAttendanceRateVO();
        vo.setScheduleId(scheduleEvent.getId());
        vo.setScheduleTitle(scheduleEvent.getTitle());
        vo.setScheduleStartTime(scheduleEvent.getStartTime());
        vo.setScheduleEndTime(scheduleEvent.getEndTime());
        vo.setParticipantCount(participantCount);
        vo.setCheckedCount(checkedCount);
        vo.setUncheckedCount(uncheckedCount);
        vo.setAttendanceRate(calculateRate(checkedCount, participantCount));
        vo.setCheckInAddress(scheduleEvent.getCheckInAddress());
        vo.setCheckInRadiusMeters(scheduleEvent.getCheckInRadiusMeters());
        return vo;
    }

    /**
     *  计算考勤率
     *
     * @param numerator
     * @param denominator
     * @return
     */
    private Double calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round(numerator * 10000D / denominator) / 100D;
    }

    private boolean isAttendanceCheckRequired(ScheduleEvent scheduleEvent) {
        return scheduleEvent != null
                && "attendance".equalsIgnoreCase(scheduleEvent.getScheduleType())
                && Objects.equals(scheduleEvent.getCheckInEnabled(), 1)
                && scheduleEvent.getCheckInLatitude() != null
                && scheduleEvent.getCheckInLongitude() != null;
    }

    // 使用Haversin公式计算距离
    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000D;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    // 保留两位小数
    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}