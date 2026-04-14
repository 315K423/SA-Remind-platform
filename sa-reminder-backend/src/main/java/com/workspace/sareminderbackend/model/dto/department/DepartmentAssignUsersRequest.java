package com.workspace.sareminderbackend.model.dto.department;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DepartmentAssignUsersRequest implements Serializable {
    private Long departmentId;
    private List<Long> userIdList;
    private static final long serialVersionUID = 1L;
}
