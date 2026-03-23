package com.workspace.sareminderbackend.model.dto.schedule;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ScheduleReminderPopupReadAllRequest implements Serializable {

    /**
     * 可选：仅标记这些任务已读；为空则把当前用户所有 sent 状态弹窗标记为已读
     */
    private List<Long> taskIdList;

    private static final long serialVersionUID = 1L;
}
