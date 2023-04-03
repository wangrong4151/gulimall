package com.atguigu.gulimall.product.netty.config;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @Description:用户id和channel的关联关系处理
 * @author wr
 * @since 2022-11-16
 */
public class UserChannelRel {
	
	private static final Logger log = LoggerFactory.getLogger(UserChannelRel.class);
		
	private static HashMap<String, Channel> manager = new HashMap<>();

	public static void put(String senderId, Channel channel) {
		manager.put(senderId, channel);
	}
	
	public static Channel get(String senderId) {
		return manager.get(senderId);
	}
		
	public static void output() {
		for (HashMap.Entry<String, Channel> entry : manager.entrySet()) {
			log.info(" imChat获得连接：  UserId: " + entry.getKey() 
							+ ", ChannelId: " + entry.getValue().id().asLongText());
		}
	}
	
}
