package com.workspace.sareminderbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class AvatarWebConfig implements WebMvcConfigurer {

    /**
     * 头像实际保存目录：项目运行目录/avatar
     */
    public static final String AVATAR_UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "avatar";

    /**
     * 头像访问地址：http://localhost:8080/api/avatar/文件名
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations(Paths.get(AVATAR_UPLOAD_DIR).toUri().toString());
    }
}