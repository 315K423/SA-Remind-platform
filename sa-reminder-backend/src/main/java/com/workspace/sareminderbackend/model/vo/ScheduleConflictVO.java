package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleConflictVO implements Serializable {
    private Long scheduleId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String conflictType;
    private Long userId;
    private String userName;
    private static final long serialVersionUID = 1L;
}
