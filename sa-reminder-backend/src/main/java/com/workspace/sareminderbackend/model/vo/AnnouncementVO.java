package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementVO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private String scopeType;
    private String status;
    private Long publisherId;
    private LocalDateTime publishTime;
    private List<Long> departmentIdList;
    private String receiveStatus;
    private LocalDateTime readTime;
    private static final long serialVersionUID = 1L;
}
