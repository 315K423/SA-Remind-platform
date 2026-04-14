package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleAttendanceUpdateRequest implements Serializable {

    private Long participantId;

    private String attendanceStatus;

    private static final long serialVersionUID = 1L;
}
