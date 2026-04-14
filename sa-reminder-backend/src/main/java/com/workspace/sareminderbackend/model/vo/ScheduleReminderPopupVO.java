package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleReminderPopupVO implements Serializable {

    private Long id;

    private Long scheduleId;

    private Long ruleId;

    private Integer remindIndex;

    private String popupTitle;

    private String popupContent;

    private LocalDateTime plannedRemindTime;

    private LocalDateTime actualRemindTime;

    private String taskStatus;

    /** 是否需要定位签到 */
    private Boolean attendanceCheckRequired;

    /** 签到地点 */
    private String checkInAddress;

    /** 签到半径 */
    private Integer checkInRadiusMeters;

    private String scheduleType;

    private static final long serialVersionUID = 1L;
}
