package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleEventVO implements Serializable {

    private Long id;
    private String title;
    private String content;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allDay;
    private String scheduleType;
    private String visibility;
    private String status;
    private Long creatorId;
    private Integer checkInEnabled;
    private String checkInAddress;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Integer checkInRadiusMeters;
    private List<Long> participantUserIdList;
    private List<Long> departmentIdList;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
