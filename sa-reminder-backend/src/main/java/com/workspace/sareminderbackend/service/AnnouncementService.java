package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementAddRequest;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementQueryRequest;
import com.workspace.sareminderbackend.model.dto.announcement.AnnouncementUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.announcement.Announcement;
import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.model.vo.AnnouncementReadRateVO;
import com.workspace.sareminderbackend.model.vo.AnnouncementVO;

import java.util.List;

public interface AnnouncementService extends IService<Announcement> {
    long addAnnouncement(AnnouncementAddRequest request, User loginUser);
    boolean updateAnnouncement(AnnouncementUpdateRequest request, User loginUser);
    boolean deleteAnnouncement(long id);
    QueryWrapper getQueryWrapper(AnnouncementQueryRequest request);
    AnnouncementVO getAnnouncementVO(Announcement announcement, User loginUser);
    List<AnnouncementVO> getAnnouncementVOList(List<Announcement> list, User loginUser);
    List<AnnouncementVO> listMyAnnouncements(User loginUser);
    boolean readAnnouncement(long announcementId, User loginUser);

    /**
     * 分页统计每条公告的已读率。
     */
    Page<AnnouncementReadRateVO> listReadRatePage(AnnouncementQueryRequest request);
}
