package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.dto.department.DepartmentAddRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentAssignUsersRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentQueryRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentTransferUsersRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentUpdateRequest;
import com.workspace.sareminderbackend.model.entity.Department;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.DepartmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    long addDepartment(DepartmentAddRequest request);

    boolean updateDepartment(DepartmentUpdateRequest request);

    boolean deleteDepartment(long id);

    QueryWrapper getQueryWrapper(DepartmentQueryRequest request);

    DepartmentVO getDepartmentVO(Department department);

    List<DepartmentVO> getDepartmentVOList(List<Department> departmentList);

    boolean assignUsers(DepartmentAssignUsersRequest request);

    boolean transferUsers(DepartmentTransferUsersRequest request);

    List<User> importUsersFromExcel(Long departmentId, String defaultRole, MultipartFile file);

    Department getValidDepartment(Long departmentId);
}
