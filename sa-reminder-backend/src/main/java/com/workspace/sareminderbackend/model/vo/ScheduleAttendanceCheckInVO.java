package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleAttendanceCheckInVO implements Serializable {

    private Boolean success;

    private Boolean withinRange;

    private Double distanceMeters;

    private String attendanceStatus;

    private String message;

    private static final long serialVersionUID = 1L;
}
