package com.workspace.sareminderbackend.model.dto.announcement;

import com.workspace.sareminderbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AnnouncementQueryRequest extends PageRequest implements Serializable {
    private Long id;
    private String title;
    private String scopeType;
    private String status;
    private Long departmentId;
    private static final long serialVersionUID = 1L;
}
