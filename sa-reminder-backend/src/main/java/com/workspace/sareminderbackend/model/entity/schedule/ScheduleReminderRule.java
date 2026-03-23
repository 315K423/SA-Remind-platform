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
 * 日程提醒策略 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("schedule_reminder_rule")
public class ScheduleReminderRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("scheduleId")
    private Long scheduleId;

    @Column("userId")
    private Long userId;

    @Column("remindOffsetMinutes")
    private Integer remindOffsetMinutes;

    @Column("repeatCount")
    private Integer repeatCount;

    @Column("repeatIntervalMinutes")
    private Integer repeatIntervalMinutes;

    @Column("popupEnabled")
    private Integer popupEnabled;

    @Column("status")
    private String status;

    @Column("lastGenerateTime")
    private LocalDateTime lastGenerateTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
