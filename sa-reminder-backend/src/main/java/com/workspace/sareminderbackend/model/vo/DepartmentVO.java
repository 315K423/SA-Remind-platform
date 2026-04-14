package com.workspace.sareminderbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DepartmentVO implements Serializable {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer userCount;
    private LocalDateTime createTime;
    private static final long serialVersionUID = 1L;
}
