package com.workspace.sareminderbackend.model.dto.department;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DepartmentTransferUsersRequest implements Serializable {
    private Long fromDepartmentId;
    private Long toDepartmentId;
    private List<Long> userIdList;
    private static final long serialVersionUID = 1L;
}
