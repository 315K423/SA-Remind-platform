package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleCalendarDayVO implements Serializable {
    private String date;
    private List<ScheduleEventVO> scheduleList = new ArrayList<>();
    private static final long serialVersionUID = 1L;
}
