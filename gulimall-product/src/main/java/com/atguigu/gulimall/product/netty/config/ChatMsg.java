package com.atguigu.gulimall.product.netty.config;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description:聊天信息
 * @author wr
 * @since 2022-11-16
 */
@Data
public class ChatMsg implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * 用户id
	 */
	private String userId;
	
	/**
	 * 发送者的用户id	
	 */
	private String sendId;		
	
	/**
	 * 接受者的用户id
	 */
	private String receiveId;
	
	/**
	 * 聊天内容
	 */
	private String msgContent;
	
	/**
	 * 消息发送时间
	 */
	private Date sendTime;
	
	/**
	 * 用于消息的签收
	 */
	private String msgId;



}
