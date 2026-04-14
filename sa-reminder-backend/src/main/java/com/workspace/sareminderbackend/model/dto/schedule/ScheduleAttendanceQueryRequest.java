package com.workspace.sareminderbackend.model.dto.schedule;

import com.workspace.sareminderbackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleAttendanceQueryRequest extends PageRequest implements Serializable {

    private Long scheduleId;
    private Long userId;
    private String scheduleTitle;
    private String userName;
    private String attendanceStatus;

    private static final long serialVersionUID = 1L;
}
