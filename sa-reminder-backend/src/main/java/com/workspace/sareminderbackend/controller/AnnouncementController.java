package com.workspace.sareminderbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.DeleteRequest;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementAddRequest;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementQueryRequest;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.announcement.Announcement;
import com.workspace.sareminderbackend.model.vo.AnnouncementVO;
import com.workspace.sareminderbackend.service.AnnouncementService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    @Resource
    private UserService userService;

    /**
     * 新增公告
     *
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addAnnouncement(@RequestBody AnnouncementAddRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(announcementService.addAnnouncement(request, loginUser));
    }

    /**
     * 更新公告
     *
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAnnouncement(@RequestBody AnnouncementUpdateRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(announcementService.updateAnnouncement(request, loginUser));
    }

    /**
     * 删除公告
     *
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAnnouncement(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(announcementService.deleteAnnouncement(request.getId()));
    }

    /**
     * 分页查询公告
     *
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AnnouncementVO>> listPage(@RequestBody AnnouncementQueryRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();
        Page<Announcement> page = announcementService.page(Page.of(pageNum, pageSize), announcementService.getQueryWrapper(request));
        Page<AnnouncementVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(announcementService.getAnnouncementVOList(page.getRecords(), loginUser));
        return ResultUtils.success(voPage);
    }

    /**
     * 查询我的公告
     *
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/my/list")
    @AuthCheck
    public BaseResponse<List<AnnouncementVO>> myList(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(announcementService.listMyAnnouncements(loginUser));
    }

    /**
     * 标记已读
     *
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/read")
    @AuthCheck
    public BaseResponse<Boolean> read(@RequestBody DeleteRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(announcementService.readAnnouncement(request.getId(), loginUser));
    }
}
