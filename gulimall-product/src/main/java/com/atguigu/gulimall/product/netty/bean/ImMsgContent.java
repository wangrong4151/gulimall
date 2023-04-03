package com.atguigu.gulimall.product.netty.bean;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天信息
 */
@Data
public class ImMsgContent {
	
	// "from": "来源ID",
	private Integer fromId;
	
	// "to": "目标ID",
	private Integer toId;
	
	// "cmd":"命令码(11)int类型",
	private Integer cmd;
	
	// "createTime": "消息创建时间long类型",
	private LocalDateTime createTime;
	
	// "msgType": "消息类型int类型(0:text、1:image、2:voice、3:vedio、4:music、5:news)",
	private Integer msgType;
	
	// "chatType":"聊天类型int类型(0:未知,1:公聊,2:私聊)",
	private Integer chatType;
	
	// "groupId":"群组id仅在chatType为(1)时需要,String类型",
	private Integer groupId;
	
	// "content": "内容",
	private String content;
	
	// "extras" : "扩展字段,JSON对象格式如：{'扩展字段名称':'扩展字段value'}"
	private String extras;


	
}
