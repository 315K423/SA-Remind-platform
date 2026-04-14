package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleEventSaveVO implements Serializable {
    private Boolean success;
    private Long eventId;
    private Boolean conflictDetected;
    private List<ScheduleConflictVO> conflictList = new ArrayList<>();
    private static final long serialVersionUID = 1L;
}
