package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleReminderRuleSaveRequest implements Serializable {

    /**
     * 为空表示新增，不为空表示更新
     */
    private Long id;

    /**
     * 日程ID
     */
    private Long scheduleId;

    /**
     * 首次提醒提前分钟数，例如 30 表示开始前30分钟
     */
    private Integer remindOffsetMinutes;

    /**
     * 额外重复提醒次数，0 表示仅提醒一次
     */
    private Integer repeatCount;

    /**
     * 重复提醒间隔分钟数
     */
    private Integer repeatIntervalMinutes;

    /**
     * 是否启用网页弹窗 0/1
     */
    private Integer popupEnabled;

    /**
     * enabled/disabled
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
