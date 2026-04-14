package com.workspace.sareminderbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.DeleteRequest;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.department.DepartmentAddRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentAssignUsersRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentQueryRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentTransferUsersRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentUpdateRequest;
import com.workspace.sareminderbackend.model.entity.Department;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.DepartmentVO;
import com.workspace.sareminderbackend.model.vo.UserVO;
import com.workspace.sareminderbackend.service.DepartmentService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    @Resource
    private UserService userService;

    /**
     * 添加部门
     *
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addDepart(@RequestBody DepartmentAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(departmentService.addDepartment(request));
    }

    /**
     * 更新部门
     *
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDepart(@RequestBody DepartmentUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(departmentService.updateDepartment(request));
    }

    /**
     * 删除部门
     *
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteDepart(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(departmentService.deleteDepartment(request.getId()));
    }

    /**
     * 获取部门
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DepartmentVO> getById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Department department = departmentService.getValidDepartment(id);
        return ResultUtils.success(departmentService.getDepartmentVO(department));
    }

    /**
     * 获取部门列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<DepartmentVO>> listPage(@RequestBody DepartmentQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long pageNum = request.getPageNum();
        long pageSize = request.getPageSize();
        Page<Department> page = departmentService.page(Page.of(pageNum, pageSize), departmentService.getQueryWrapper(request));
        Page<DepartmentVO> voPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        voPage.setRecords(departmentService.getDepartmentVOList(page.getRecords()));
        return ResultUtils.success(voPage);
    }

    /**
     * 分配用户
     *
     * @param request
     * @return
     */
    @PostMapping("/assign/users")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> assignUsers(@RequestBody DepartmentAssignUsersRequest request) {
        ThrowUtils.throwIf(request == null || request.getDepartmentId() == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(departmentService.assignUsers(request));
    }

    /**
     * 转移用户
     *
     * @param request
     * @return
     */
    @PostMapping("/transfer/users")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> transferUsers(@RequestBody DepartmentTransferUsersRequest request) {
        ThrowUtils.throwIf(request == null || request.getFromDepartmentId() == null || request.getToDepartmentId() == null,
                ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(departmentService.transferUsers(request));
    }

    /**
     * 导入用户
     *
     * @param file
     * @param departmentId
     * @param defaultRole
     * @return
     */
    @PostMapping(value = "/import/users", consumes = {"multipart/form-data"})
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<UserVO>> importUsers(@RequestPart("file") MultipartFile file,
                                                  @RequestParam("departmentId") Long departmentId,
                                                  @RequestParam(value = "defaultRole", required = false) String defaultRole) {
        ThrowUtils.throwIf(departmentId == null || departmentId <= 0, ErrorCode.PARAMS_ERROR);
        List<User> userList = departmentService.importUsersFromExcel(departmentId, defaultRole, file);
        return ResultUtils.success(userService.getUserVOList(userList));
    }
}
