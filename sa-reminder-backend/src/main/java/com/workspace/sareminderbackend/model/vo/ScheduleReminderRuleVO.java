package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleReminderRuleVO implements Serializable {

    private Long id;

    private Long scheduleId;

    private Long userId;

    private String scheduleTitle;

    private LocalDateTime scheduleStartTime;

    private Integer remindOffsetMinutes;

    private Integer repeatCount;

    private Integer repeatIntervalMinutes;

    private Integer popupEnabled;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
