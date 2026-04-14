package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventAddRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleEventUpdateRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleEvent;
import com.workspace.sareminderbackend.model.vo.ScheduleCalendarDayVO;
import com.workspace.sareminderbackend.model.vo.ScheduleEventSaveVO;
import com.workspace.sareminderbackend.model.vo.ScheduleEventVO;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleEventService extends IService<ScheduleEvent> {

    ScheduleEventSaveVO addScheduleEvent(ScheduleEventAddRequest request, User loginUser);

    ScheduleEventSaveVO updateScheduleEvent(ScheduleEventUpdateRequest request, User loginUser);

    boolean deleteScheduleEvent(long id, User loginUser);

    QueryWrapper getQueryWrapper(ScheduleEventQueryRequest request, User loginUser);

    ScheduleEventVO getScheduleEventVO(ScheduleEvent scheduleEvent);

    List<ScheduleEventVO> getScheduleEventVOList(List<ScheduleEvent> list);

    List<ScheduleEventVO> listMyMonthSchedule(int year, int month, User loginUser);

    ScheduleCalendarDayVO getMyDaySchedule(LocalDate date, User loginUser);
}
