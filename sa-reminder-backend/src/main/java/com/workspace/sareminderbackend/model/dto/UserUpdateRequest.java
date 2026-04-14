package com.workspace.sareminderbackend.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateRequest implements Serializable {

    private Long id;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private Long departmentId;

    private static final long serialVersionUID = 1L;
}
