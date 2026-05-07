package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤率统计 VO。
 */
@Data
public class ScheduleAttendanceRateVO implements Serializable {

    private Long scheduleId;
    private String scheduleTitle;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;

    /** 参与人数 */
    private Integer participantCount;

    /** 已考勤人数 */
    private Integer checkedCount;

    /** 未考勤人数 */
    private Integer uncheckedCount;

    /** 考勤率，范围 0-100 */
    private Double attendanceRate;

    private String checkInAddress;
    private Integer checkInRadiusMeters;

    private static final long serialVersionUID = 1L;
}
