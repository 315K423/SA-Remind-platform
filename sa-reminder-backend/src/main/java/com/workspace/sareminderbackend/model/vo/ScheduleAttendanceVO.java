package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleAttendanceVO implements Serializable {

    private Long participantId;
    private Long scheduleId;
    private String scheduleTitle;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private Long userId;
    private String userName;
    private String participantRole;
    private String responseStatus;
    private String attendanceStatus;
    private LocalDateTime checkInTime;
    private String checkInAddress;
    private Integer checkInRadiusMeters;
    private Double checkInDistanceMeters;

    private static final long serialVersionUID = 1L;
}
