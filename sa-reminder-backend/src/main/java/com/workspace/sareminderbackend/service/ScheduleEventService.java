package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventAddRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventUpdateRequest;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.ScheduleEventVO;

import java.util.List;

public interface ScheduleEventService extends IService<ScheduleEvent> {

    /**
     * 创建日程（包含参与人写入）
     */
    long addScheduleEvent(ScheduleEventAddRequest request, User loginUser);

    /**
     * 更新日程（可选覆盖参与人）
     */
    boolean updateScheduleEvent(ScheduleEventUpdateRequest request, User loginUser);

    /**
     * 删除日程
     */
    boolean deleteScheduleEvent(long id, User loginUser);

    /**
     * 获取查询条件（会自动做权限收敛）
     */
    QueryWrapper getQueryWrapper(ScheduleEventQueryRequest request, User loginUser);

    ScheduleEventVO getScheduleEventVO(ScheduleEvent scheduleEvent);

    List<ScheduleEventVO> getScheduleEventVOList(List<ScheduleEvent> list);
}
