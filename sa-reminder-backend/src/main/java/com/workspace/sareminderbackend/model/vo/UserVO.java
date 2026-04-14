package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
