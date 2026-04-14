package com.workspace.sareminderbackend.model.dto.department;

import lombok.Data;

import java.io.Serializable;

@Data
public class DepartmentUpdateRequest implements Serializable {
    private Long id;
    private String name;
    private String code;
    private String description;
    private static final long serialVersionUID = 1L;
}
