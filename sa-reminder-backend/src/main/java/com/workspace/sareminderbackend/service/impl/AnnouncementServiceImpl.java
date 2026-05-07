package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.AnnouncementDepartmentMapper;
import com.workspace.sareminderbackend.mapper.AnnouncementMapper;
import com.workspace.sareminderbackend.mapper.AnnouncementReceiverMapper;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementAddRequest;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementQueryRequest;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.announcement.Announcement;
import com.workspace.sareminderbackend.model.entity.announcement.AnnouncementDepartment;
import com.workspace.sareminderbackend.model.entity.announcement.AnnouncementReceiver;
import com.workspace.sareminderbackend.model.vo.AnnouncementReadRateVO;
import com.workspace.sareminderbackend.model.vo.AnnouncementVO;
import com.workspace.sareminderbackend.service.AnnouncementService;
import com.workspace.sareminderbackend.service.DepartmentService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private static final String SCOPE_ALL = "all";
    private static final String SCOPE_DEPARTMENT = "department";
    private static final String STATUS_PUBLISHED = "published";
    private static final String RECEIVE_UNREAD = "unread";
    private static final String RECEIVE_READ = "read";

    @Resource
    private AnnouncementDepartmentMapper announcementDepartmentMapper;

    @Resource
    private AnnouncementReceiverMapper announcementReceiverMapper;

    @Resource
    private UserService userService;

    @Resource
    private DepartmentService departmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addAnnouncement(AnnouncementAddRequest request, User loginUser) {
        validateRequest(request.getTitle(), request.getContent(), request.getScopeType(), request.getDepartmentIdList());

        Announcement announcement = new Announcement();
        BeanUtil.copyProperties(request, announcement);

        LocalDateTime now = LocalDateTime.now();
        announcement.setPublisherId(loginUser.getId());
        // AddRequest 当前没有 status 字段，这里直接默认发布
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setPublishTime(now);
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        announcement.setIsDelete(0);

        boolean ok = this.save(announcement);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增公告失败");
        }

        // 只同步部门关系，不再生成未读快照
        syncDepartmentRelations(announcement.getId(), request.getScopeType(), request.getDepartmentIdList());
        return announcement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnnouncement(AnnouncementUpdateRequest request, User loginUser) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "公告 id 非法");
        }

        Announcement old = this.getById(request.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "公告不存在");
        }

        String finalTitle = StrUtil.blankToDefault(request.getTitle(), old.getTitle());
        String finalContent = StrUtil.blankToDefault(request.getContent(), old.getContent());
        String finalScopeType = StrUtil.blankToDefault(request.getScopeType(), old.getScopeType());
        List<Long> finalDepartmentIdList = request.getDepartmentIdList() == null
                ? getDepartmentIds(old.getId())
                : request.getDepartmentIdList();

        validateRequest(finalTitle, finalContent, finalScopeType, finalDepartmentIdList);

        Announcement update = new Announcement();
        BeanUtil.copyProperties(request, update);
        update.setId(old.getId());
        update.setUpdateTime(LocalDateTime.now());

        boolean ok = this.updateById(update);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新公告失败");
        }

        // 只同步部门关系，不再重建 receiver
        syncDepartmentRelations(old.getId(), finalScopeType, finalDepartmentIdList);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnouncement(long id) {
        announcementDepartmentMapper.deleteByQuery(
                QueryWrapper.create().eq("announcementId", id)
        );
        announcementReceiverMapper.deleteByQuery(
                QueryWrapper.create().eq("announcementId", id)
        );
        return this.removeById(id);
    }

    @Override
    public QueryWrapper getQueryWrapper(AnnouncementQueryRequest request) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("id", request.getId())
                .like("title", request.getTitle())
                .eq("scopeType", request.getScopeType())
                .eq("status", request.getStatus());

        if (request.getDepartmentId() != null) {
            QueryWrapper subQuery = QueryWrapper.create()
                    .select("announcementId")
                    .from("announcement_department")
                    .eq("departmentId", request.getDepartmentId())
                    .eq("isDelete", 0);
            wrapper.in("id", subQuery);
        }

        if (StrUtil.isNotBlank(request.getSortField())) {
            wrapper.orderBy(request.getSortField(), "ascend".equals(request.getSortOrder()));
        } else {
            wrapper.orderBy("publishTime", false);
        }
        return wrapper;
    }

    @Override
    public AnnouncementVO getAnnouncementVO(Announcement announcement, User loginUser) {
        if (announcement == null) {
            return null;
        }

        AnnouncementVO vo = new AnnouncementVO();
        BeanUtil.copyProperties(announcement, vo);
        vo.setDepartmentIdList(getDepartmentIds(announcement.getId()));

        // 默认未读
        vo.setReceiveStatus(RECEIVE_UNREAD);
        vo.setReadTime(null);

        if (loginUser != null && loginUser.getId() != null) {
            AnnouncementReceiver receiver = announcementReceiverMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("announcementId", announcement.getId())
                            .eq("userId", loginUser.getId())
                            .eq("isDelete", 0)
            );
            if (receiver != null && RECEIVE_READ.equalsIgnoreCase(receiver.getReceiveStatus())) {
                vo.setReceiveStatus(RECEIVE_READ);
                vo.setReadTime(receiver.getReadTime());
            }
        }

        return vo;
    }

    @Override
    public List<AnnouncementVO> getAnnouncementVOList(List<Announcement> list, User loginUser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        List<Long> announcementIds = list.stream()
                .map(Announcement::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, List<Long>> announcementDepartmentMap = buildAnnouncementDepartmentMap(announcementIds);
        Map<Long, AnnouncementReceiver> readReceiverMap = buildReadReceiverMap(announcementIds, loginUser);

        List<AnnouncementVO> result = new ArrayList<>();
        for (Announcement announcement : list) {
            AnnouncementVO vo = new AnnouncementVO();
            BeanUtil.copyProperties(announcement, vo);

            vo.setDepartmentIdList(
                    announcementDepartmentMap.getOrDefault(announcement.getId(), new ArrayList<>())
            );

            vo.setReceiveStatus(RECEIVE_UNREAD);
            vo.setReadTime(null);

            AnnouncementReceiver receiver = readReceiverMap.get(announcement.getId());
            if (receiver != null) {
                vo.setReceiveStatus(RECEIVE_READ);
                vo.setReadTime(receiver.getReadTime());
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public List<AnnouncementVO> listMyAnnouncements(User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 动态查询：直接查所有已发布公告，再在 Java 中按 scope 动态过滤
        List<Announcement> publishedAnnouncements = this.list(
                QueryWrapper.create()
                        .eq("status", STATUS_PUBLISHED)
                        .eq("isDelete", 0)
                        .orderBy("publishTime", false)
        );

        if (CollUtil.isEmpty(publishedAnnouncements)) {
            return new ArrayList<>();
        }

        List<Announcement> filteredList = publishedAnnouncements.stream()
                .filter(announcement -> canUserAccessAnnouncement(announcement, loginUser))
                .collect(Collectors.toList());

        return getAnnouncementVOList(filteredList, loginUser);
    }

    @Override
    public boolean readAnnouncement(long announcementId, User loginUser) {
        if (announcementId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "公告 id 非法");
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Announcement announcement = this.getById(announcementId);
        if (announcement == null || (announcement.getIsDelete() != null && announcement.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "公告不存在");
        }
        if (!STATUS_PUBLISHED.equals(announcement.getStatus())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前公告未发布，无法阅读");
        }
        if (!canUserAccessAnnouncement(announcement, loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前公告未分发给你");
        }

        // receiver 表现在只存“已读”
        AnnouncementReceiver receiver = announcementReceiverMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("announcementId", announcementId)
                        .eq("userId", loginUser.getId())
        );

        LocalDateTime now = LocalDateTime.now();

        if (receiver == null) {
            AnnouncementReceiver newReceiver = new AnnouncementReceiver();
            newReceiver.setAnnouncementId(announcementId);
            newReceiver.setUserId(loginUser.getId());
            newReceiver.setReceiveStatus(RECEIVE_READ);
            newReceiver.setReadTime(now);
            newReceiver.setCreateTime(now);
            newReceiver.setUpdateTime(now);
            newReceiver.setIsDelete(0);
            return announcementReceiverMapper.insert(newReceiver) > 0;
        }

        AnnouncementReceiver update = new AnnouncementReceiver();
        update.setId(receiver.getId());
        update.setReceiveStatus(RECEIVE_READ);
        update.setReadTime(now);
        update.setUpdateTime(now);
        update.setIsDelete(0);
        return announcementReceiverMapper.update(update) > 0;
    }

    @Override
    public Page<AnnouncementReadRateVO> listReadRatePage(AnnouncementQueryRequest request) {
        AnnouncementQueryRequest finalRequest = request == null ? new AnnouncementQueryRequest() : request;
        long pageNum = finalRequest.getPageNum() <= 0 ? 1 : finalRequest.getPageNum();
        long pageSize = finalRequest.getPageSize() <= 0 ? 10 : finalRequest.getPageSize();

        Page<Announcement> announcementPage = this.page(Page.of(pageNum, pageSize), getQueryWrapper(finalRequest));
        Page<AnnouncementReadRateVO> voPage = new Page<>(pageNum, pageSize, announcementPage.getTotalRow());

        List<User> userList = userService.list(QueryWrapper.create().eq("isDelete", 0));
        List<AnnouncementReadRateVO> voList = announcementPage.getRecords().stream()
                .map(announcement -> buildReadRateVO(announcement, userList))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    private AnnouncementReadRateVO buildReadRateVO(Announcement announcement, List<User> userList) {
        List<User> receiverUserList = Optional.ofNullable(userList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(user -> canUserAccessAnnouncement(announcement, user))
                .collect(Collectors.toList());

        Set<Long> receiverUserIdSet = receiverUserList.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> readUserIdSet = new HashSet<>();
        if (!receiverUserIdSet.isEmpty()) {
            List<AnnouncementReceiver> readReceiverList = announcementReceiverMapper.selectListByQuery(
                    QueryWrapper.create()
                            .eq("announcementId", announcement.getId())
                            .eq("receiveStatus", RECEIVE_READ)
                            .eq("isDelete", 0)
                            .in("userId", receiverUserIdSet)
            );
            readUserIdSet = readReceiverList.stream()
                    .map(AnnouncementReceiver::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        int receiverCount = receiverUserIdSet.size();
        int readCount = readUserIdSet.size();
        int unreadCount = Math.max(receiverCount - readCount, 0);

        AnnouncementReadRateVO vo = new AnnouncementReadRateVO();
        vo.setAnnouncementId(announcement.getId());
        vo.setTitle(announcement.getTitle());
        vo.setScopeType(announcement.getScopeType());
        vo.setStatus(announcement.getStatus());
        vo.setPublisherId(announcement.getPublisherId());
        vo.setPublishTime(announcement.getPublishTime());
        vo.setReceiverCount(receiverCount);
        vo.setReadCount(readCount);
        vo.setUnreadCount(unreadCount);
        vo.setReadRate(calculateRate(readCount, receiverCount));

        if (announcement.getPublisherId() != null) {
            User publisher = userService.getById(announcement.getPublisherId());
            if (publisher != null) {
                vo.setPublisherName(publisher.getUserName());
            }
        }
        return vo;
    }

    private Double calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round(numerator * 10000D / denominator) / 100D;
    }

    /**
     * 判断当前用户是否有权限看到这条公告
     */
    private boolean canUserAccessAnnouncement(Announcement announcement, User loginUser) {
        if (announcement == null || loginUser == null) {
            return false;
        }
        if (!STATUS_PUBLISHED.equals(announcement.getStatus())) {
            return false;
        }
        if (announcement.getIsDelete() != null && announcement.getIsDelete() == 1) {
            return false;
        }

        // 全公司公告：所有登录用户可见
        if (SCOPE_ALL.equals(announcement.getScopeType())) {
            return true;
        }

        // 部门公告：当前用户必须有部门，且命中关系表
        if (!SCOPE_DEPARTMENT.equals(announcement.getScopeType())) {
            return false;
        }
        if (loginUser.getDepartmentId() == null) {
            return false;
        }

        AnnouncementDepartment relation = announcementDepartmentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("announcementId", announcement.getId())
                        .eq("departmentId", loginUser.getDepartmentId())
                        .eq("isDelete", 0)
        );
        return relation != null;
    }

    /**
     * 只同步公告-部门关系，不再生成接收人快照
     */
    private void syncDepartmentRelations(Long announcementId, String scopeType, List<Long> departmentIdList) {
        announcementDepartmentMapper.deleteByAnnouncementIdPhysically(announcementId);

        if (SCOPE_ALL.equals(scopeType)) {
            return;
        }

        List<Long> finalDepartmentIds = Optional.ofNullable(departmentIdList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        for (Long departmentId : finalDepartmentIds) {
            departmentService.getValidDepartment(departmentId);

            AnnouncementDepartment relation = new AnnouncementDepartment();
            relation.setAnnouncementId(announcementId);
            relation.setDepartmentId(departmentId);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            relation.setIsDelete(0);
            announcementDepartmentMapper.insert(relation);
        }
    }

    private Map<Long, List<Long>> buildAnnouncementDepartmentMap(List<Long> announcementIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (CollUtil.isEmpty(announcementIds)) {
            return result;
        }

        List<AnnouncementDepartment> relationList = announcementDepartmentMapper.selectListByQuery(
                QueryWrapper.create()
                        .in("announcementId", announcementIds)
                        .eq("isDelete", 0)
        );

        for (AnnouncementDepartment relation : relationList) {
            result.computeIfAbsent(relation.getAnnouncementId(), k -> new ArrayList<>())
                    .add(relation.getDepartmentId());
        }
        return result;
    }

    /**
     * receiver 只取“已读记录”
     */
    private Map<Long, AnnouncementReceiver> buildReadReceiverMap(List<Long> announcementIds, User loginUser) {
        Map<Long, AnnouncementReceiver> result = new HashMap<>();
        if (CollUtil.isEmpty(announcementIds) || loginUser == null || loginUser.getId() == null) {
            return result;
        }

        List<AnnouncementReceiver> receiverList = announcementReceiverMapper.selectListByQuery(
                QueryWrapper.create()
                        .in("announcementId", announcementIds)
                        .eq("userId", loginUser.getId())
                        .eq("isDelete", 0)
                        .eq("receiveStatus", RECEIVE_READ)
        );

        for (AnnouncementReceiver receiver : receiverList) {
            result.put(receiver.getAnnouncementId(), receiver);
        }
        return result;
    }

    private List<Long> getDepartmentIds(Long announcementId) {
        List<AnnouncementDepartment> relationList = announcementDepartmentMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("announcementId", announcementId)
                        .eq("isDelete", 0)
        );
        return relationList.stream()
                .map(AnnouncementDepartment::getDepartmentId)
                .collect(Collectors.toList());
    }

    private void validateRequest(String title, String content, String scopeType, List<Long> departmentIdList) {
        if (StrUtil.hasBlank(title, content, scopeType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "公告标题、内容、范围不能为空");
        }
        if (!SCOPE_ALL.equals(scopeType) && !SCOPE_DEPARTMENT.equals(scopeType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "scopeType 仅支持 all/department");
        }
        if (SCOPE_DEPARTMENT.equals(scopeType) && CollUtil.isEmpty(departmentIdList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门公告必须指定部门");
        }
    }
}