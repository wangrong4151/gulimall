package com.atguigu.gulimall.product.netty;

import com.atguigu.gulimall.product.netty.config.NettyServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyBooter implements ApplicationListener<ContextRefreshedEvent>{

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		log.info("event.getApplicationContext().getParent()......{}",event.getApplicationContext().getParent());
		if (event.getApplicationContext().getParent().getParent() == null) {
			try {
				NettyServer.getInstance().start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
