package com.workspace.sareminderbackend.model.dto.schedule;

import com.workspace.sareminderbackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class ScheduleReminderRuleQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private Long scheduleId;

    /**
     * 管理员可按用户查询
     */
    private Long userId;

    private String status;

    private static final long serialVersionUID = 1L;
}
