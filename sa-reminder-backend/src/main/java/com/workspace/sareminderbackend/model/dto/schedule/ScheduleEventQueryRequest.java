package com.workspace.sareminderbackend.model.dto.schedule;

import com.workspace.sareminderbackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleEventQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private String title;
    private String scheduleType;
    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;
    private Long creatorId;
    private Long departmentId;
    private Long participantUserId;

    private static final long serialVersionUID = 1L;
}
