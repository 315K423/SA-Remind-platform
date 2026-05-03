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
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
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
        String encryptPassword = getEncryptPassword(userPassword);
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
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
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
