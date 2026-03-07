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

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 标题
     */
    @Column("title")
    private String title;

    /**
     * 内容/备注
     */
    @Column("content")
    private String content;

    /**
     * 地点
     */
    @Column("location")
    private String location;

    /**
     * 开始时间
     */
    @Column("startTime")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column("endTime")
    private LocalDateTime endTime;

    /**
     * 是否全天 0-否 1-是
     */
    @Column("allDay")
    private Integer allDay;

    /**
     * 日程类型：personal/company
     */
    @Column("scheduleType")
    private String scheduleType;

    /**
     * 可见性：private/public
     */
    @Column("visibility")
    private String visibility;

    /**
     * 状态：normal/cancelled
     */
    @Column("status")
    private String status;

    /**
     * 创建人
     */
    @Column("creatorId")
    private Long creatorId;

    /**
     * 编辑时间
     */
    @Column("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
