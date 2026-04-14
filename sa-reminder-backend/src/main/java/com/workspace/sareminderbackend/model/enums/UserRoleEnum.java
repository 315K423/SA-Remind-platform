package com.workspace.sareminderbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("普通员工", "user", 1),
    MANAGER("部门经理", "manager", 2),
    ADMIN("管理员", "admin", 3);

    private final String text;
    private final String value;
    private final int level;

    UserRoleEnum(String text, String value, int level) {
        this.text = text;
        this.value = value;
        this.level = level;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public static boolean hasRole(String loginRole, String mustRole) {
        if (mustRole == null || mustRole.isBlank()) {
            return true;
        }
        UserRoleEnum loginRoleEnum = getEnumByValue(loginRole);
        UserRoleEnum mustRoleEnum = getEnumByValue(mustRole);
        if (loginRoleEnum == null || mustRoleEnum == null) {
            return false;
        }
        return loginRoleEnum.level >= mustRoleEnum.level;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isManager() {
        return this == MANAGER;
    }
}
