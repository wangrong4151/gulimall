package com.atguigu.gulimall.product.netty.enums;

/**
 * 
 * @Description: 发送消息的动作 枚举
 * 1, "第一次(或重连)初始化连接"		2, "聊天消息"		3, "消息签收"		4, "客户端保持心跳"
 * 5, "拉取好友"				6, "好友申请"	friend_request		7, "群消息"
 */
public enum MsgActionEnum {
	
	/**
	 * 1, "第一次(或重连)初始化连接"
	 */
	CONNECT(1, "第一次(或重连)初始化连接"),
	
	/**
	 * 2, "聊天消息"
	 */
	CHAT(2, "聊天消息"),
	
	/**
	 * 3, "消息签收"
	 */
	SIGNED(3, "消息签收"),
	
	/**
	 * 4, "客户端保持心跳"
	 */
	KEEPALIVE(4, "客户端保持心跳"),
	
	/**
	 * 5, "拉取好友"
	 */
	PULL_FRIEND(5, "拉取好友"),
	
	/**
	 * 6, "好友申请"	friend_request
	 */
	FRIEND_REQUEST(6, "好友申请"),
	
	/**
	 * 7, "群消息"
	 */
	GROUP_MSG(7, "群消息");
	
	public final Integer type;
	public final String content;
	
	MsgActionEnum(Integer type, String content){
		this.type = type;
		this.content = content;
	}
	
	public Integer getType() {
		return type;
	}  
}
