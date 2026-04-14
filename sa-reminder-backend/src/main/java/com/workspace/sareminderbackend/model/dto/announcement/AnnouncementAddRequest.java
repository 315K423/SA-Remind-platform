package com.workspace.sareminderbackend.model.dto.announcement;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AnnouncementAddRequest implements Serializable {
    private String title;
    private String content;
    private String scopeType;
    private List<Long> departmentIdList;
    private static final long serialVersionUID = 1L;
}
