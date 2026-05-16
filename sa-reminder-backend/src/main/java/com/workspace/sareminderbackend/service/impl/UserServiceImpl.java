package com.workspace.sareminderbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.mapper.DepartmentMapper;
import com.workspace.sareminderbackend.mapper.UserMapper;
import com.workspace.sareminderbackend.model.dto.UserQueryRequest;
import com.workspace.sareminderbackend.model.entity.Department;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.model.vo.LoginUserVO;
import com.workspace.sareminderbackend.model.vo.UserVO;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.workspace.sareminderbackend.constant.UserConstant.USER_LOGIN_STATE;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public long userRegister(String userAccount,String userName, String userPassword, String checkPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
        }

        // 密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 构建用户实体
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setEditTime(LocalDateTime.now());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户注册失败,数据库错误");
        }
        return user.getId();
    }

    /**
     * 将user实体转化为过滤后的loginUserVO
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        fillDepartmentInfo(user.getDepartmentId(), loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 存入session
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // todo
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        fillDepartmentInfo(user.getDepartmentId(), userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        return QueryWrapper.create()
                .eq("id", userQueryRequest.getId())
                .eq("userRole", userQueryRequest.getUserRole())
                .eq("departmentId", userQueryRequest.getDepartmentId())
                .like("userAccount", userQueryRequest.getUserAccount())
                .like("userName", userQueryRequest.getUserName())
                .like("userProfile", userQueryRequest.getUserProfile())
                .orderBy(userQueryRequest.getSortField(), "ascend".equals(userQueryRequest.getSortOrder()));
    }

    /**
     *  密码加密
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "aicode";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<User> listByDepartmentId(Long departmentId) {
        if (departmentId == null) {
            return new ArrayList<>();
        }
        return this.list(QueryWrapper.create().eq("departmentId", departmentId));
    }

    /**
     *  验证用户是否属于指定部门
     *
     * @param user
     * @param departmentId
     */
    @Override
    public void validateDepartmentUser(User user, Long departmentId) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (!departmentId.equals(user.getDepartmentId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户不属于指定部门");
        }
    }

    @Override
    public boolean isAdmin(User user) {
        return UserRoleEnum.ADMIN.equals(UserRoleEnum.getEnumByValue(user.getUserRole()));
    }

    @Override
    public boolean isManager(User user) {
        return UserRoleEnum.MANAGER.equals(UserRoleEnum.getEnumByValue(user.getUserRole()));
    }

    /**
     *  导出excel表
     *
     * @param userQueryRequest 查询条件，和用户管理列表保持一致
     * @param response         HTTP 响应，用于输出 xlsx 文件流
     */
    @Override
    public void exportUserExcel(UserQueryRequest userQueryRequest, HttpServletResponse response) {
        UserQueryRequest finalRequest = userQueryRequest == null ? new UserQueryRequest() : userQueryRequest;
        List<UserVO> userVOList = getUserVOList(this.list(getQueryWrapper(finalRequest)));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户数据");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"用户ID", "账号", "用户名", "角色", "部门", "个人简介", "创建时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充用户数据
            for (int i = 0; i < userVOList.size(); i++) {
                UserVO userVO = userVOList.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(userVO.getId() == null ? "" : String.valueOf(userVO.getId()));
                row.createCell(1).setCellValue(blankToDash(userVO.getUserAccount()));
                row.createCell(2).setCellValue(blankToDash(userVO.getUserName()));
                row.createCell(3).setCellValue(blankToDash(userVO.getUserRole()));
                row.createCell(4).setCellValue(blankToDash(userVO.getDepartmentName()));
                row.createCell(5).setCellValue(blankToDash(userVO.getUserProfile()));
                row.createCell(6).setCellValue(userVO.getCreateTime() == null ? "" : userVO.getCreateTime().toString().replace("T", " "));
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应头和文件名
            String fileName = URLEncoder.encode("用户数据_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户 Excel 导出失败");
        }
    }

    private String blankToDash(String value) {
        return StrUtil.isBlank(value) ? "-" : value;
    }

    /**
     *  填充部门信息到UserVO或者loginUserVO
     *
     * @param departmentId
     * @param target
     */
    private void fillDepartmentInfo(Long departmentId, Object target) {
        if (departmentId == null) {
            return;
        }
        Department department = departmentMapper.selectOneById(departmentId);
        if (department == null) {
            return;
        }
        if (target instanceof UserVO userVO) {
            userVO.setDepartmentName(department.getName());
        }
        if (target instanceof LoginUserVO loginUserVO) {
            loginUserVO.setDepartmentName(department.getName());
        }
    }
}
