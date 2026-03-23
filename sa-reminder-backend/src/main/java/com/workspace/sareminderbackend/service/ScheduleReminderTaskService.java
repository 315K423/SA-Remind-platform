package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderPopupVO;

import java.util.List;

public interface ScheduleReminderTaskService extends IService<ScheduleReminderTask> {

    /**
     * 获取当前用户待弹窗提醒列表
     */
    List<ScheduleReminderPopupVO> listPopupTasks(User loginUser);

    /**
     * 标记弹窗已读
     */
    boolean readPopupTask(long taskId, User loginUser);

    /**
     * 全部标记已读
     */
    boolean readAllPopupTasks(List<Long> taskIdList, User loginUser);
}
