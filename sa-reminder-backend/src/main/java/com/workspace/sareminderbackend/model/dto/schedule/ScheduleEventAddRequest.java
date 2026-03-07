package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleEventAddRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容/备注
     */
    private String content;

    /**
     * 地点
     */
    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 是否全天 0/1
     */
    private Integer allDay;

    /**
     * personal/company（company 仅管理员可用）
     */
    private String scheduleType;

    /**
     * private/public
     */
    private String visibility;

    /**
     * 参与人 userId 列表（可选）。
     * - 普通员工：仅允许添加自己（或留空，系统自动加自己）
     * - 管理员：可添加多人
     */
    private List<Long> participantUserIdList;

    private static final long serialVersionUID = 1L;
}
