package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleEventUpdateRequest implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer allDay;

    /**
     * normal/cancelled
     */
    private String status;

    /**
     * private/public
     */
    private String visibility;

    /**
     * 可选：覆盖式更新参与人列表（仅管理员或创建人可用）。
     * 传 null 表示不修改；传空列表表示清空（会自动保留创建人）。
     */
    private List<Long> participantUserIdList;

    private static final long serialVersionUID = 1L;
}
