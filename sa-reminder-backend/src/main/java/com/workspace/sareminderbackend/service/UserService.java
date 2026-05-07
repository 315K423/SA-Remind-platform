package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.dto.UserQueryRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.vo.LoginUserVO;
import com.workspace.sareminderbackend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface UserService extends IService<User> {

    long userRegister(String userAccount,String userName, String userPassword, String checkPassword);

    LoginUserVO getLoginUserVO(User user);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    boolean userLogout(HttpServletRequest request);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String UserPassword);

    List<User> listByDepartmentId(Long departmentId);

    void validateDepartmentUser(User user, Long departmentId);

    boolean isAdmin(User user);

    boolean isManager(User user);

    /**
     * 导出用户 Excel。
     *
     * @param userQueryRequest 查询条件，和用户管理列表保持一致
     * @param response         HTTP 响应，用于输出 xlsx 文件流
     */
    void exportUserExcel(UserQueryRequest userQueryRequest, HttpServletResponse response);
}
