package com.atguigu.gulimall.coupon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;


/**
 * @Author wr
 * @Date 2021/8/26 14:56
 * @Version 1.0
 */
@Configuration
public class GulimallSessionConfig {
    @Bean // redis的json序列化
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    @Bean // cookie
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("GULISESSIONID"); // cookie的键
        serializer.setDomainName("gulimall.com"); // 扩大session作用域，也就是cookie的有效域
        return serializer;
    }
}
