package com.atguigu.gulimall.product.netty.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.netty.bean.ImChatMsgLogs;
import com.atguigu.gulimall.product.netty.service.ImChatMsgLogsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * im_chat聊天记录 前端控制器
 * </p>
 *
 * @author wr
 * @since 2022-11-16
 */
@RestController
@RequestMapping("/imChatMsgLogs")
@Api(tags = "im_chat聊天记录")
public class ImChatMsgLogsController {
	
	@Autowired
	private ImChatMsgLogsService imChatMsgLogsService;
	
	@ApiOperation(value = "查询好友聊天记录",response = ImChatMsgLogs.class)
	@RequestMapping(value="/selectFriendLogsList",method = RequestMethod.GET)
	public R selectFriendLogsList(String sendId,String receiveId){

		QueryWrapper<ImChatMsgLogs> queryWrapper = new QueryWrapper<ImChatMsgLogs>();
		queryWrapper.eq("to_type", 1);		
		//queryWrapper.eq("send_id", sendId).or().eq("receive_id", sendId);
		queryWrapper.and(wrapper -> wrapper.eq("receive_id", receiveId).or().eq("send_id", receiveId));
		queryWrapper.and(wrapper -> wrapper.eq("send_id", sendId).or().eq("receive_id", sendId));
		//queryWrapper.eq("receive_id", receiveId).or().eq("send_id", receiveId);
		List<ImChatMsgLogs> infoList = imChatMsgLogsService.list(queryWrapper);
		return R.ok().put("infoList",infoList);
	}
	
	@ApiOperation(value = "查询群聊天记录",response = ImChatMsgLogs.class)
	@RequestMapping(value="/selectGroupLogsList",method = RequestMethod.GET)
	public R selectGroupLogsList(Integer groupInfoId){

		QueryWrapper<ImChatMsgLogs> queryWrapper = new QueryWrapper<ImChatMsgLogs>();
		queryWrapper.eq("group_info_id", groupInfoId);
		queryWrapper.eq("data_status", 1);
		List<ImChatMsgLogs> infoList = imChatMsgLogsService.list(queryWrapper);
		return R.ok().put("infoList",infoList);
	}
}
