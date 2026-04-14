package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleAttendanceCheckInRequest implements Serializable {

    private Long taskId;

    private Double latitude;

    private Double longitude;

    private static final long serialVersionUID = 1L;
}
