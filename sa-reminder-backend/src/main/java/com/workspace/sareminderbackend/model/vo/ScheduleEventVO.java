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

    /**
     * 参与人 userId 列表（简化版）
     */
    private List<Long> participantUserIdList;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
