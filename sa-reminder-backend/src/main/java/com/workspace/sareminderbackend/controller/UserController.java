package com.workspace.sareminderbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.common.BaseResponse;
import com.workspace.sareminderbackend.common.DeleteRequest;
import com.workspace.sareminderbackend.common.ResultUtils;
import com.workspace.sareminderbackend.config.AvatarWebConfig;
import com.workspace.sareminderbackend.constant.UserConstant;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.exception.ThrowUtils;
import com.workspace.sareminderbackend.model.dto.*;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.model.vo.LoginUserVO;
import com.workspace.sareminderbackend.model.vo.UserVO;
import com.workspace.sareminderbackend.service.DepartmentService;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private DepartmentService departmentService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest.getUserAccount(),userRegisterRequest.getUserName(),userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword());
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLogin(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword(), request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 上传头像接口
     *
     * @param file
     * @param userAccount
     * @param request
     * @return
     */
    @PostMapping("/avatar/upload")
    @AuthCheck
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "userAccount", required = false) String userAccount,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "头像文件不能为空");

        User loginUser = userService.getLoginUser(request);

        // 将账号中不允许的字符替换为下划线，生成安全文件名前缀
        // 允许字符：英文字母、数字、下划线、短横线、汉字
        String accountForName = StrUtil.blankToDefault(userAccount, loginUser.getUserAccount());
        String safeAccount = accountForName.replaceAll("[^a-zA-Z0-9_\\-一-龥]", "_");

        String originalFilename = file.getOriginalFilename();
        String suffix = getFileSuffix(originalFilename);
        Set<String> allowedSuffixSet = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        ThrowUtils.throwIf(!allowedSuffixSet.contains(suffix), ErrorCode.PARAMS_ERROR, "仅支持 JPG、PNG、GIF、WEBP 格式图片");

        String fileName = safeAccount + "_" + IdUtil.getSnowflakeNextId() + suffix;
        File uploadDir = new File(AvatarWebConfig.AVATAR_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean mkdirResult = uploadDir.mkdirs();
            ThrowUtils.throwIf(!mkdirResult, ErrorCode.OPERATION_ERROR, "头像目录创建失败");
        }

        File targetFile = new File(uploadDir, fileName);
        try {
            file.transferTo(targetFile); // 保存文件
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "头像保存失败");
        }

        // 构建头像访问URL
        String avatarUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath() + "/avatar/" + fileName;
        return ResultUtils.success(avatarUrl);
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        if (userAddRequest.getDepartmentId() != null) {
            departmentService.getValidDepartment(userAddRequest.getDepartmentId());
        }
        UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(userAddRequest.getUserRole());
        ThrowUtils.throwIf(roleEnum == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        user.setUserPassword(userService.getEncryptPassword("12345678"));
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setEditTime(now);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    @GetMapping("/get/vo")
    @AuthCheck
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        if (!userService.isAdmin(loginUser) && !loginUser.getId().equals(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userUpdateRequest.getDepartmentId() != null) {
            departmentService.getValidDepartment(userUpdateRequest.getDepartmentId());
        }
        if (userUpdateRequest.getUserRole() != null && UserRoleEnum.getEnumByValue(userUpdateRequest.getUserRole()) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色非法");
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        user.setUpdateTime(LocalDateTime.now());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户更新个人信息
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    @AuthCheck
    public BaseResponse<LoginUserVO> updateMyUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(StrUtil.isBlank(userUpdateRequest.getUserName()), ErrorCode.PARAMS_ERROR, "用户名不能为空");

        User user = new User();
        user.setId(loginUser.getId());
        user.setUserName(userUpdateRequest.getUserName());
        user.setUserAvatar(userUpdateRequest.getUserAvatar());
        user.setUserProfile(userUpdateRequest.getUserProfile());
        user.setUpdateTime(LocalDateTime.now());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        User newLoginUser = userService.getById(loginUser.getId());
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, newLoginUser);
        return ResultUtils.success(userService.getLoginUserVO(newLoginUser));
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        // 1. 校验请求参数不能为空
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3. 获取当前登录用户角色
        String userRole = loginUser.getUserRole();

        // 4. 判断是否为管理员或项目经理
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(userRole);
        boolean isManager = UserConstant.MANAGER_ROLE.equals(userRole);

        // 5. 如果既不是管理员，也不是项目经理，则不允许访问该接口
        ThrowUtils.throwIf(!isAdmin && !isManager, ErrorCode.NO_AUTH_ERROR);

        // 6. 如果是项目经理，只允许查询自己部门的用户
        if (isManager) {
            ThrowUtils.throwIf(loginUser.getDepartmentId() == null, ErrorCode.NO_AUTH_ERROR, "当前项目经理未绑定部门");

            // 强制把查询部门改成项目经理自己的部门，防止前端传其他部门 id 越权查询
            userQueryRequest.setDepartmentId(loginUser.getDepartmentId());
        }

        // 7. 获取分页参数
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();

        // 8. 根据查询条件分页查询用户
        Page<User> userPage = userService.page(
                Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest)
        );

        // 9. 创建用户 VO 分页对象
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());

        // 10. 将 User 实体列表转换成 UserVO 列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());

        // 11. 设置分页 records
        userVOPage.setRecords(userVOList);

        // 12. 返回统一响应结果
        return ResultUtils.success(userVOPage);
    }

    /**
     * 导出用户 Excel。
     * 注意：导出接口不返回 BaseResponse，而是直接输出 xlsx 文件流。
     */
    @GetMapping("/admin/export")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void exportUserExcel(UserQueryRequest userQueryRequest, HttpServletResponse response) {
        userService.exportUserExcel(userQueryRequest, response);
    }

    /**
     * 提取文件后缀，统一小写
     *
     * @param originalFilename
     * @return
     */
    private String getFileSuffix(String originalFilename) {
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }
}
