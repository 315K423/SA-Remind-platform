package com.workspace.sareminderbackend.controller;

import com.workspace.sareminderbackend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/scan")
    public void scan(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        emailService.sendEmail(to, subject, content);
        System.out.println("邮件发送成功，收件人：" + to);
    }
}
