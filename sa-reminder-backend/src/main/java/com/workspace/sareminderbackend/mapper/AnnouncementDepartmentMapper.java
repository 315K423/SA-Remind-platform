package com.workspace.sareminderbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.workspace.sareminderbackend.model.entity.announcement.AnnouncementDepartment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementDepartmentMapper extends BaseMapper<AnnouncementDepartment> {

    @Delete("DELETE FROM announcement_department WHERE announcementId = #{announcementId}")
    int deleteByAnnouncementIdPhysically(@Param("announcementId") Long announcementId);
}
