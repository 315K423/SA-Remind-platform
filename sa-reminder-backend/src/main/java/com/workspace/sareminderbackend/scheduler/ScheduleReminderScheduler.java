package com.workspace.sareminderbackend.scheduler;

import com.workspace.sareminderbackend.service.ScheduleReminderRuleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日程提醒定时调度器。
 */
@Slf4j
@Component
public class ScheduleReminderScheduler {

    @Resource
    private ScheduleReminderRuleService scheduleReminderRuleService;

    /**
     * 每分钟扫描一次。
     */
    @Scheduled(fixedDelay = 60000)
    public void scanAndDispatch() {
        try {
            scheduleReminderRuleService.scanAndDispatchOnce();
        } catch (Exception e) {
            log.error("schedule reminder scan failed", e);
        }
    }
}
