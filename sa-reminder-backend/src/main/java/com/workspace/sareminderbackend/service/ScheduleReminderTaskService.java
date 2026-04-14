package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderPopupVO;

import java.util.List;

public interface ScheduleReminderTaskService extends IService<ScheduleReminderTask> {
    List<ScheduleReminderPopupVO> listPopupTasks(User loginUser);
    boolean readPopupTask(long taskId, User loginUser);
    boolean readAllPopupTasks(List<Long> taskIdList, User loginUser);
}
