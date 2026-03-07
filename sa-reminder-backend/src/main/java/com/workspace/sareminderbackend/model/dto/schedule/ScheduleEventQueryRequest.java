package com.workspace.sareminderbackend.model.dto.schedule;

import com.workspace.sareminderbackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleEventQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String title;

    /**
     * personal/company
     */
    private String scheduleType;

    /**
     * 时间范围过滤（可选）
     */
    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;

    /**
     * 创建人（管理员可用）
     */
    private Long creatorId;

    private static final long serialVersionUID = 1L;
}
