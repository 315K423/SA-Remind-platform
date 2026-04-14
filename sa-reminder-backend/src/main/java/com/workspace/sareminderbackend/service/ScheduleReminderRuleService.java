package com.workspace.sareminderbackend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderRuleQueryRequest;
import com.workspace.sareminderbackend.model.dto.schedule.ScheduleReminderRuleSaveRequest;
import com.workspace.sareminderbackend.model.entity.User;
import com.workspace.sareminderbackend.model.entity.schedule.ScheduleReminderRule;
import com.workspace.sareminderbackend.model.vo.ScheduleReminderRuleVO;

import java.util.List;

public interface ScheduleReminderRuleService extends IService<ScheduleReminderRule> {
    long saveOrUpdateRule(ScheduleReminderRuleSaveRequest request, User loginUser);
    boolean deleteRule(long id, User loginUser);
    QueryWrapper getQueryWrapper(ScheduleReminderRuleQueryRequest request, User loginUser);
    ScheduleReminderRuleVO getScheduleReminderRuleVO(ScheduleReminderRule rule);
    List<ScheduleReminderRuleVO> getScheduleReminderRuleVOList(List<ScheduleReminderRule> list);
    void scanAndDispatchOnce();
}
