package com.workspace.sareminderbackend.model.entity.schedule;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日程事件 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("schedule_event")
public class ScheduleEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("title")
    private String title;

    @Column("content")
    private String content;

    @Column("location")
    private String location;

    @Column("startTime")
    private LocalDateTime startTime;

    @Column("endTime")
    private LocalDateTime endTime;

    @Column("allDay")
    private Integer allDay;

    @Column("scheduleType")
    private String scheduleType;

    @Column("visibility")
    private String visibility;

    @Column("status")
    private String status;

    @Column("creatorId")
    private Long creatorId;

    /** 定位签到是否启用 0/1 */
    @Column("checkInEnabled")
    private Integer checkInEnabled;

    /** 签到地点描述 */
    @Column("checkInAddress")
    private String checkInAddress;

    /** 签到目标纬度 */
    @Column("checkInLatitude")
    private Double checkInLatitude;

    /** 签到目标经度 */
    @Column("checkInLongitude")
    private Double checkInLongitude;

    /** 签到半径（米） */
    @Column("checkInRadiusMeters")
    private Integer checkInRadiusMeters;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
