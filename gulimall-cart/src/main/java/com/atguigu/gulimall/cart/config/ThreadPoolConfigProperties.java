package com.atguigu.gulimall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
//@Component
@ConfigurationProperties(prefix = "gulimall.thread")
public class ThreadPoolConfigProperties {


    private Integer coreSize=20;


    private Integer maxSize=200;


    private Integer keepAliveTime=10;

}
