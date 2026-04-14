package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.DepartmentMapper;
import com.workspace.sareminderbackend.model.dto.department.DepartmentAddRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentAssignUsersRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentQueryRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentTransferUsersRequest;
import com.workspace.sareminderbackend.model.dto.department.DepartmentUpdateRequest;
import com.workspace.sareminderbackend.model.entity.Department;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.model.vo.DepartmentVO;
import com.workspace.sareminderbackend.service.DepartmentService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Resource
    private UserService userService;

    @Override
    public long addDepartment(DepartmentAddRequest request) {
        validateDepartmentName(request.getName(), null);
        Department department = new Department();
        BeanUtil.copyProperties(request, department);
        LocalDateTime now = LocalDateTime.now();
        department.setCreateTime(now);
        department.setUpdateTime(now);
        department.setIsDelete(0);
        boolean ok = this.save(department);
        if (!ok) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增部门失败");
        }
        return department.getId();
    }

    @Override
    public boolean updateDepartment(DepartmentUpdateRequest request) {
        Department old = getValidDepartment(request.getId());
        validateDepartmentName(StrUtil.blankToDefault(request.getName(), old.getName()), request.getId());
        Department update = new Department();
        BeanUtil.copyProperties(request, update);
        update.setUpdateTime(LocalDateTime.now());
        return this.updateById(update);
    }

    @Override
    public boolean deleteDepartment(long id) {
        Department department = getValidDepartment(id);
        List<User> userList = userService.listByDepartmentId(id);
        if (CollUtil.isNotEmpty(userList)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "部门下仍有员工，不能直接删除");
        }
        return this.removeById(department.getId());
    }

    @Override
    public QueryWrapper getQueryWrapper(DepartmentQueryRequest request) {
        return QueryWrapper.create()
                .eq("id", request.getId())
                .like("name", request.getName())
                .like("code", request.getCode())
                .orderBy(request.getSortField(), "ascend".equals(request.getSortOrder()));
    }

    @Override
    public DepartmentVO getDepartmentVO(Department department) {
        if (department == null) {
            return null;
        }
        DepartmentVO vo = new DepartmentVO();
        BeanUtil.copyProperties(department, vo);
        vo.setUserCount(userService.listByDepartmentId(department.getId()).size());
        return vo;
    }

    @Override
    public List<DepartmentVO> getDepartmentVOList(List<Department> departmentList) {
        if (CollUtil.isEmpty(departmentList)) {
            return new ArrayList<>();
        }
        return departmentList.stream().map(this::getDepartmentVO).collect(Collectors.toList());
    }

    @Override
    public boolean assignUsers(DepartmentAssignUsersRequest request) {
        Department department = getValidDepartment(request.getDepartmentId());
        List<Long> userIdList = Optional.ofNullable(request.getUserIdList()).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (userIdList.isEmpty()) {
            return true;
        }
        for (Long userId : userIdList) {
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在: " + userId);
            }
            User update = new User();
            update.setId(userId);
            update.setDepartmentId(department.getId());
            userService.updateById(update);
        }
        return true;
    }

    @Override
    public boolean transferUsers(DepartmentTransferUsersRequest request) {
        if (Objects.equals(request.getFromDepartmentId(), request.getToDepartmentId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原部门与目标部门不能相同");
        }
        getValidDepartment(request.getFromDepartmentId());
        Department targetDepartment = getValidDepartment(request.getToDepartmentId());
        List<Long> userIdList = Optional.ofNullable(request.getUserIdList()).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (userIdList.isEmpty()) {
            return true;
        }
        for (Long userId : userIdList) {
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在: " + userId);
            }
            if (!Objects.equals(user.getDepartmentId(), request.getFromDepartmentId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 " + userId + " 不属于原部门");
            }
            User update = new User();
            update.setId(userId);
            update.setDepartmentId(targetDepartment.getId());
            userService.updateById(update);
        }
        return true;
    }

    @Override
    public List<User> importUsersFromExcel(Long departmentId, String defaultRole, MultipartFile file) {
        getValidDepartment(departmentId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件为空");
        }
        List<User> savedUsers = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "excel 内容为空");
            }
            Map<String, Integer> headIndexMap = parseHeadIndexMap(sheet.getRow(0));
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                String userAccount = getStringCellValue(row, headIndexMap, "userAccount");
                String userName = getStringCellValue(row, headIndexMap, "userName");
                String userRole = StrUtil.blankToDefault(getStringCellValue(row, headIndexMap, "userRole"), defaultRole);
                String userProfile = getStringCellValue(row, headIndexMap, "userProfile");
                String userAvatar = getStringCellValue(row, headIndexMap, "userAvatar");
                if (StrUtil.hasBlank(userAccount, userName)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "第 " + (i + 1) + " 行账号或姓名为空");
                }
                if (this.userService.count(QueryWrapper.create().eq("userAccount", userAccount)) > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在: " + userAccount);
                }
                UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(StrUtil.blankToDefault(userRole, UserRoleEnum.USER.getValue()));
                if (roleEnum == null || UserRoleEnum.ADMIN.equals(roleEnum)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "excel 中 userRole 非法，且不允许批量导入 admin");
                }
                User user = new User();
                user.setUserAccount(userAccount);
                user.setUserName(userName);
                user.setUserRole(roleEnum.getValue());
                user.setUserProfile(userProfile);
                user.setUserAvatar(userAvatar);
                user.setDepartmentId(departmentId);
                user.setUserPassword(userService.getEncryptPassword("12345678"));
                boolean ok = userService.save(user);
                if (!ok) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "导入用户失败: " + userAccount);
                }
                savedUsers.add(user);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析 excel 失败: " + e.getMessage());
        }
        return savedUsers;
    }

    @Override
    public Department getValidDepartment(Long departmentId) {
        if (departmentId == null || departmentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门 id 非法");
        }
        Department department = this.getById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "部门不存在");
        }
        return department;
    }

    private void validateDepartmentName(String name, Long excludeId) {
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门名称不能为空");
        }
        QueryWrapper wrapper = QueryWrapper.create().eq("name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门名称已存在");
        }
    }

    private Map<String, Integer> parseHeadIndexMap(Row headRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headRow) {
            map.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }
        return map;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && StrUtil.isNotBlank(cell.toString())) {
                return false;
            }
        }
        return true;
    }

    private String getStringCellValue(Row row, Map<String, Integer> headIndexMap, String columnName) {
        Integer index = headIndexMap.get(columnName);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        cell.setCellType(CellType.STRING);
        return StrUtil.trimToNull(cell.getStringCellValue());
    }
}
