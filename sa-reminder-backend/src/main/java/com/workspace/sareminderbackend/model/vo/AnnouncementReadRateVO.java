package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告读取率统计 VO。
 */
@Data
public class AnnouncementReadRateVO implements Serializable {

    private Long announcementId;
    private String title;
    private String scopeType;
    private String status;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;

    /** 应接收人数 */
    private Integer receiverCount;

    /** 已读人数 */
    private Integer readCount;

    /** 未读人数 */
    private Integer unreadCount;

    /** 已读率，范围 0-100 */
    private Double readRate;

    private static final long serialVersionUID = 1L;
}
