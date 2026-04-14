package com.workspace.sareminderbackend.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddRequest implements Serializable {

    private String userName;
    private String userAccount;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private Long departmentId;

    private static final long serialVersionUID = 1L;
}
