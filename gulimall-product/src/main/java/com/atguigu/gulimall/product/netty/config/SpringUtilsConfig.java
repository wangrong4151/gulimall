package com.atguigu.gulimall.product.netty.config;

import com.atguigu.gulimall.product.netty.utils.SpringBeanUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringUtilsConfig {
    @Bean
    public SpringBeanUtil getSpringBeanUtil() {
        return new SpringBeanUtil();
    }
}
