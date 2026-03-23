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

    private static final long serialVersionUID = 1L;
}
