package com.workspace.sareminderbackend.model.dto.department;

import com.workspace.sareminderbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class DepartmentQueryRequest extends PageRequest implements Serializable {
    private Long id;
    private String name;
    private String code;
    private static final long serialVersionUID = 1L;
}
