package com.workspace.sareminderbackend.aop;

import com.workspace.sareminderbackend.annotation.AuthCheck;
import com.workspace.sareminderbackend.exception.BusinessException;
import com.workspace.sareminderbackend.exception.ErrorCode;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.enums.UserRoleEnum;
import com.workspace.sareminderbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;


    /**
     * 拦截所有带有AuthCheck注解的方法
     * @param joinPoint 切入点
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，直接放行
        if (mustRole == null || mustRole.isBlank()) {
            return joinPoint.proceed();
        }
        // 以下的代码，必须有这个权限才能通行
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，直接拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 需要管理员权限，但当前登录用户没有
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过普通用户的权限校验，放行
        return joinPoint.proceed();
    }
}
