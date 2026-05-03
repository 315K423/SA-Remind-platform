package com.workspace.sareminderbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 日程提醒任务 映射层
 */
public interface ScheduleReminderTaskMapper extends BaseMapper<ScheduleReminderTask> {

    @Insert("""
        INSERT INTO schedule_reminder_task
        (
            id,
            ruleId,
            scheduleId,
            userId,
            remindIndex,
            plannedRemindTime,
            actualRemindTime,
            taskStatus,
            popupTitle,
            popupContent,
            readTime,
            createTime,
            updateTime,
            isDelete
        )
        VALUES
        (
            #{task.id},
            #{task.ruleId},
            #{task.scheduleId},
            #{task.userId},
            #{task.remindIndex},
            #{task.plannedRemindTime},
            #{task.actualRemindTime},
            #{task.taskStatus},
            #{task.popupTitle},
            #{task.popupContent},
            #{task.readTime},
            #{task.createTime},
            #{task.updateTime},
            #{task.isDelete}
        )
        ON DUPLICATE KEY UPDATE
            scheduleId = VALUES(scheduleId),
            userId = VALUES(userId),
            plannedRemindTime = VALUES(plannedRemindTime),
            actualRemindTime = VALUES(actualRemindTime),
            taskStatus = VALUES(taskStatus),
            popupTitle = VALUES(popupTitle),
            popupContent = VALUES(popupContent),
            readTime = VALUES(readTime),
            updateTime = VALUES(updateTime),
            isDelete = VALUES(isDelete)
        """)
    int upsertTask(@Param("task") ScheduleReminderTask task);
}