package com.atguigu.gulimall.product.netty.config;


import com.atguigu.gulimall.product.netty.utils.SpringBeanUtil;
import com.atguigu.gulimall.product.netty.bean.ImChatMsgLogs;
import com.atguigu.gulimall.product.netty.enums.MsgActionEnum;
import com.atguigu.gulimall.product.netty.service.ImChatMsgLogsService;
import com.atguigu.gulimall.product.netty.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Description:处理消息的handler
 * 				TextWebSocketFrame： 在netty中，是用于为websocket专门处理文本的对象，frame是消息的载体
 * SimpleChannelInboundHandler：	对于请求来讲 ，相当于 【入站，入境】
 * @author wr
 * @since 2022-11-16
 */
public class NettyWsChannelInboundHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>{
	
	private static final Logger log = LoggerFactory.getLogger(NettyWsChannelInboundHandler.class);
	
	/**
	 * 用于记录和管理所有客户端的channle
	 */
	public static ChannelGroup users = 
			new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	/**
	 * 用户信息列表
	 */
	/*public static List<FriendInfo> friendList = new ArrayList<FriendInfo>();*/
	
	/**
	 * 从channel缓冲区读数据   { "action":1, "chatMsg":{ "userId": 1, "sendId": 1, "receiveId": 1, "msgContent": "sdagsdgasg" }, "extand": "" }
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) 
			throws Exception {
		// 获得   channel
		Channel currentChannel = ctx.channel();
		// 获取客户端传输过来的消息
		String content = msg.text();
			
		// 1. 获取客户端发来的消息
		DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
		Integer action = dataContent.getAction();
		
		// 2. 判断消息类型，根据不同的类型来处理不同的业务
		if (action.equals(MsgActionEnum.CONNECT.type)) {
			log.info("第一次消息连接。。。");
			// 	2.1  当websocket 第一次open的时候，初始化channel，把用的channel和userid关联起来
			String sendId = dataContent.getChatMsg().getSendId();
			UserChannelRel.put(sendId, currentChannel);
			UserChannelRel.output();
			
		} else if (action.equals(MsgActionEnum.CHAT.type)) {
			//  2.2  聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]
			log.info("接受到消息。。。。。");
			ImChatMsgLogsService imChatMsgLogsService = (ImChatMsgLogsService) SpringBeanUtil.getBean("imChatMsgLogsServiceImpl");
			ChatMsg chatMsg = dataContent.getChatMsg();
			String sendId = chatMsg.getSendId();
			String receiveId = chatMsg.getReceiveId();
			String msgContent = chatMsg.getMsgContent();
			String userId = chatMsg.getUserId();
				
			// 保存消息到数据库，并且标记为 未签收
			ImChatMsgLogs logs = new ImChatMsgLogs();
			logs.setUserId(userId);
			logs.setSendId(sendId);
			logs.setReceiveId(receiveId);
			logs.setMsgContent(msgContent);
			logs.setToType(1);
			// 获取ip地址
			InetSocketAddress rAddress = (InetSocketAddress)ctx.channel().remoteAddress();
			logs.setIpReceiveLocation(rAddress.getAddress().getHostAddress());
			InetSocketAddress SAddress = (InetSocketAddress)ctx.channel().localAddress();
			logs.setIpSendLocation(SAddress.getAddress().getHostAddress());

			String msgId = imChatMsgLogsService.saveWebMsgLogs(logs);
			chatMsg.setMsgId(msgId);
			// 消息发送时间
			chatMsg.setSendTime(new Date());
			DataContent dataContentMsg = new DataContent();
			dataContentMsg.setChatMsg(chatMsg);
			// 给自己发送成功消息
			Channel sendIdChannel = UserChannelRel.get(sendId);
			sendIdChannel.writeAndFlush(
					new TextWebSocketFrame(
							JsonUtils.objectToJson(dataContent)));
			
			// 发送消息 从全局用户Channel关系中获取接受方的channel
			Channel receiverChannel = UserChannelRel.get(receiveId);
			
			if (receiverChannel == null) {
				// TODO channel为空代表用户离线，推送消息（JPush，个推，小米推送）   添加离线消息记录
				log.info(" 用户离线1 ... receiverChannel 是  null");
				imChatMsgLogsService.updateOfflineStatusTwo(msgId);
			} else {
				// 当receiverChannel不为空的时候，从ChannelGroup去查找对应的channel是否存在
				Channel findChannel = users.find(receiverChannel.id());
				if (findChannel != null) {
					// 用户在线		
					receiverChannel.writeAndFlush(
							new TextWebSocketFrame(
									JsonUtils.objectToJson(dataContent)));
				} else {
					// 用户离线 TODO 推送消息	 添加离线消息记录
					log.info(" 用户离线2 ... findChannel 是  null");
					imChatMsgLogsService.updateOfflineStatusTwo(msgId);
				}
			}	
		} else if (action.equals(MsgActionEnum.SIGNED.type)) {
			log.info(" 消息签收.....  ");
			
			// 扩展字段在signed类型的消息中，代表需要去签收的消息id，逗号间隔
			String msgIdsStr = dataContent.getExtand();
			String msgIds[] = msgIdsStr.split(",");
			
			List<String> msgIdList = new ArrayList<>();
			for (String mid : msgIds) {
				if (StringUtils.isNotBlank(mid)) {
					msgIdList.add(mid);
				}
			}	
			if (msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0) {
				//  2.3  签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态[已签收]
				// 批量签收
				ImChatMsgLogsService imChatMsgLogsService = (ImChatMsgLogsService)SpringBeanUtil.getBean("imChatMsgLogsServiceImpl");
				imChatMsgLogsService.updateMsgReadStatusOne(msgIdList);
			}
			
		} else if (action.equals(MsgActionEnum.KEEPALIVE.type)) {
			//  2.4  心跳类型的消息		
		    log.info("收到来自channel为[" + currentChannel + "]的心跳包...");
		}else if (action.equals(MsgActionEnum.GROUP_MSG.type)) {
			// 群消息发送
			log.info("发送群消息。。。。。");
			ImChatMsgLogsService imChatMsgLogsService = (ImChatMsgLogsService)SpringBeanUtil.getBean("imChatMsgLogsServiceImpl");
			ChatMsg chatMsg = dataContent.getChatMsg();
			String sendId = chatMsg.getSendId();
			String receiveId = chatMsg.getReceiveId();
			String msgContent = chatMsg.getMsgContent();
			String userId = chatMsg.getUserId();
				
			// 保存消息到数据库，并且标记为 未签收
			ImChatMsgLogs logs = new ImChatMsgLogs();
			logs.setUserId(userId);
			logs.setSendId(sendId);
			// 1 是测试群	
			logs.setGroupInfoId(1);
			logs.setReceiveId(receiveId);
			logs.setMsgContent(msgContent);
			logs.setToType(2);
			String msgId = imChatMsgLogsService.saveWebMsgLogs(logs);
			chatMsg.setMsgId(msgId.toString());
			// 消息发送时间	
			chatMsg.setSendTime(new Date());
			DataContent dataContentMsg = new DataContent();
			dataContentMsg.setChatMsg(chatMsg);
			// 给所有在线的 im用户 发送信息
			for (Channel c : users) {
				c.writeAndFlush(	
						new TextWebSocketFrame(
								JsonUtils.objectToJson(dataContent)));
			}	
			// 更新消息状态为已读	
			log.info(" 群消息发送... users.size = "+users.size());
		}
	}
	
	/**
	 * 当客户端连接服务端之后（打开连接）
	 * 获取客户端的channle，并且放到ChannelGroup中去进行管理
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		users.add(ctx.channel());
		log.info(" netty 获得连接.....	");
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		String channelId = ctx.channel().id().asShortText();
		// 当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
		users.remove(ctx.channel());
		log.info("客户端被移除，channelId为：" + channelId);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		// 发生异常之后关闭连接（关闭channel），随后从ChannelGroup中移除
		ctx.channel().close();
		users.remove(ctx.channel());
		log.info(" netty 异常了...... ");
	}


}
