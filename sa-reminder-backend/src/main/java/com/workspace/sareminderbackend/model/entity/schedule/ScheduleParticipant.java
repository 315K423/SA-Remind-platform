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
 * 日程参与人 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("schedule_participant")
public class ScheduleParticipant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("scheduleId")
    private Long scheduleId;

    @Column("userId")
    private Long userId;

    @Column("participantRole")
    private String participantRole;

    @Column("responseStatus")
    private String responseStatus;

    /** 考勤状态：not_checked/checked_in */
    @Column("attendanceStatus")
    private String attendanceStatus;

    @Column("checkInTime")
    private LocalDateTime checkInTime;

    @Column("checkInLatitude")
    private Double checkInLatitude;

    @Column("checkInLongitude")
    private Double checkInLongitude;

    @Column("checkInDistanceMeters")
    private Double checkInDistanceMeters;

    @Column("joinTime")
    private LocalDateTime joinTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
