package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleCalendarQueryRequest implements Serializable {
    private Integer year;
    private Integer month;
    private static final long serialVersionUID = 1L;
}
