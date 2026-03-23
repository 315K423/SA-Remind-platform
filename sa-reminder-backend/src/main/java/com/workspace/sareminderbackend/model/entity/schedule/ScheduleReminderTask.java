package com.workspace.sareminderbackend.model.entity.schedule;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日程提醒任务 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("schedule_reminder_task")
public class ScheduleReminderTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("ruleId")
    private Long ruleId;

    @Column("scheduleId")
    private Long scheduleId;

    @Column("userId")
    private Long userId;

    @Column("remindIndex")
    private Integer remindIndex;

    @Column("plannedRemindTime")
    private LocalDateTime plannedRemindTime;

    @Column("actualRemindTime")
    private LocalDateTime actualRemindTime;

    @Column("taskStatus")
    private String taskStatus;

    @Column("popupTitle")
    private String popupTitle;

    @Column("popupContent")
    private String popupContent;

    @Column("readTime")
    private LocalDateTime readTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
